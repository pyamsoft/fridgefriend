/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.fridge.detail.item.fridge

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.detail.DetailConstants
import com.pyamsoft.fridge.detail.list.DetailListInteractor
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.fridge.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.item.fridge.DetailItemViewEvent.PickDate
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Named

class DetailItemViewModel @Inject internal constructor(
  item: FridgeItem,
  @Named("item_editable") isEditable: Boolean,
  private val interactor: DetailListInteractor,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
  private val dateSelectBus: EventBus<DateSelectPayload>,
  private val realtime: FridgeItemRealtime
) : UiViewModel<DetailItemViewState, DetailItemViewEvent, DetailItemControllerEvent>(
    initialState = DetailItemViewState(throwable = null, item = item, isEditable = isEditable)
) {

  private val itemEntryId = item.entryId()
  private val itemId = item.id()

  private var updateDisposable by singleDisposable()
  private var deleteDisposable by singleDisposable()

  private var dateDisposable by singleDisposable()
  private var realtimeDisposable by singleDisposable()

  override fun handleViewEvent(event: DetailItemViewEvent) {
    return when (event) {
      is CommitName -> commitName(event.oldItem, event.name)
      is CommitPresence -> commitPresence(event.oldItem, event.presence)
      is ExpandItem -> expandItem(event.item)
      is PickDate -> pickDate(event.oldItem, event.year, event.month, event.day)
    }
  }

  override fun onCleared() {
    dateDisposable.tryDispose()
    realtimeDisposable.tryDispose()
  }

  fun beginObservingItem() {
    dateDisposable = dateSelectBus.listen()
        .filter { it.oldItem.entryId() == itemEntryId }
        .filter { it.oldItem.id() == itemId }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { commitDate(it.oldItem, it.year, it.month, it.day) }

    realtimeDisposable =
      Observable.merge(realtime.listenForChanges(itemEntryId), fakeRealtime.listen())
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe { handleRealtimeEvent(it) }
  }

  private fun handleRealtimeEvent(event: FridgeItemChangeEvent) {
    return when (event) {
      is Update -> handleModelUpdate(event.item)
      is Insert -> handleModelUpdate(event.item)
      else -> Unit
    }
  }

  private fun handleModelUpdate(newItem: FridgeItem) {
    if (itemId == newItem.id()) {
      setState { copy(item = newItem) }
    }
  }

  private fun pickDate(
    oldItem: FridgeItem,
    year: Int,
    month: Int,
    day: Int
  ) {
    Timber.d("Launch date picker from date: $year ${month + 1} $day")
    publish(DatePick(oldItem, year, month, day))
  }

  @CheckResult
  private fun isReadyToBeReal(item: FridgeItem): Boolean {
    return isNameValid(item.name())
  }

  private fun commitName(
    oldItem: FridgeItem,
    name: String
  ) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    if (isNameValid(name)) {
      commitItem(item = oldItem.name(name))
    } else {
      Timber.w("Invalid name: $name")
      handleInvalidName(name)
    }
  }

  private fun commitDate(
    oldItem: FridgeItem,
    year: Int,
    month: Int,
    day: Int
  ) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    Timber.d("Attempt save time: $year/${month + 1}/$day")
    val newTime = Calendar.getInstance()
        .apply {
          set(Calendar.YEAR, year)
          set(Calendar.MONTH, month)
          set(Calendar.DAY_OF_MONTH, day)
        }
        .time
    Timber.d("Save expire time: $newTime")
    commitItem(item = oldItem.expireTime(newTime))
  }

  private fun commitPresence(
    oldItem: FridgeItem,
    presence: Presence
  ) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    commitItem(item = oldItem.presence(presence))
  }

  private fun commitItem(item: FridgeItem) {
    // If this item is not real, its an empty placeholder
    // Right now, isReal is decided when an item has a non blank name.
    // Once an item is in the db, it is always real
    // The user may commit a potential update of things like the Presence or Expire Date before name
    // Keep it prepared but do not commit it until a name comes in
    // Directly call the realtime commit callback as if the
    // commit had actually happened
    if (!item.isReal() && !isReadyToBeReal(item)) {
      Timber.w("Commit called on a non-real item: $item, fake callback")
      handleFakeCommit(item)
      return
    }

    // A delete operation will stop an update operation
    updateDisposable = Completable.complete()
        .delay(DetailConstants.COMMIT_TIMEOUT_DURATION, DetailConstants.COMMIT_TIMEOUT_UNIT)
        .observeOn(AndroidSchedulers.mainThread())
        .observeOn(Schedulers.io())
        .andThen(interactor.commit(item.makeReal()))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { updateDisposable.tryDispose() }
        .subscribe({ handleClearFixMessage() }, {
          Timber.e(it, "Error updating item: ${item.id()}")
          handleError(it)
        })
  }

  private fun handleFakeCommit(item: FridgeItem) {
    fakeRealtime.publish(Insert(item))
    Timber.w("Not ready to commit item yet: $item")
    handleClearFixMessage()
  }

  private fun handleFakeDelete(item: FridgeItem) {
    fakeRealtime.publish(FridgeItemChangeEvent.Delete(item))
  }

  private fun handleInvalidName(name: String) {
    setFixMessage("ERROR: Name $name is invalid. Please fix.")
  }

  private fun handleError(throwable: Throwable) {
    setState {
      copy(throwable = throwable)
    }
  }

  private fun handleClearFixMessage() {
    setFixMessage("")
  }

  private fun setFixMessage(message: String) {
    setState {
      copy(throwable = if (message.isBlank()) null else IllegalArgumentException(message))
    }
  }

  fun archiveSelf(item: FridgeItem) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    // If this item is not real, its an empty placeholder
    // The user may still wish to delete it from their list
    // in case they have too many placeholders.
    // Directly call the realtime delete callback as if the
    // delete had actually happened
    if (!item.isReal()) {
      Timber.w("Archive called on a non-real item: $item, fake callback")
      handleFakeDelete(item)
      return
    }

    deleteDisposable = interactor.archive(item)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { deleteDisposable.tryDispose() }
        .subscribe({ }, {
          Timber.e(it, "Error archiving item: ${item.id()}")
          handleError(it)
        })
  }

  private fun expandItem(item: FridgeItem) {
    publish(ExpandDetails(item))
  }

}
