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
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailConstants
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DetailItemScope
import com.pyamsoft.fridge.detail.item.fridge.DetailItemPresenter.DetailState
import com.pyamsoft.pydroid.arch.Presenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@DetailItemScope
internal class DetailItemPresenter @Inject internal constructor(
  private val interactor: CreationListInteractor,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : Presenter<DetailState, DetailItemPresenter.Callback>(),
  DetailListItem.Callback {

  private var updateDisposable by singleDisposable()
  private var deleteDisposable by singleDisposable()

  override fun initialState(): DetailState {
    return DetailState(throwable = null)
  }

  override fun onBind() {
  }

  override fun onUnbind() {
    // Don't clear Disposables here as it may need to outlive the View
    // since the final commit happens as the View is tearing down
  }

  @CheckResult
  private fun isReadyToBeReal(item: FridgeItem): Boolean {
    return item.name().isNotBlank() && item.expireTime().time > 0L
  }

  override fun commitItem(item: FridgeItem) {
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
      .andThen(interactor.commit(item))
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { updateDisposable.tryDispose() }
      .subscribe({ }, {
        Timber.e(it, "Error updating item: ${item.id()}")
        handleError(it)
      })
  }

  fun deleteSelf(item: FridgeItem) {
    // Stop any pending updates
    updateDisposable.tryDispose()

    // If this item is not real, its an empty placeholder
    // The user may still wish to delete it from their list
    // in case they have too many placeholders.
    // Directly call the realtime delete callback as if the
    // delete had actually happened
    if (!item.isReal()) {
      Timber.w("Delete called on a non-real item: $item, fake callback")
      handleFakeDelete(item)
      return
    }

    deleteDisposable = interactor.delete(item)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { deleteDisposable.tryDispose() }
      .subscribe({ }, {
        Timber.e(it, "Error deleting item: ${item.id()}")
        handleError(it)
      })
  }

  private fun handleFakeCommit(item: FridgeItem) {
    fakeRealtime.publish(FridgeItemChangeEvent.Insert(item))
  }

  private fun handleFakeDelete(item: FridgeItem) {
    fakeRealtime.publish(FridgeItemChangeEvent.Delete(item))
  }

  private fun handleError(throwable: Throwable) {
    setState {
      copy(throwable = throwable)
    }
  }

  data class DetailState(val throwable: Throwable?)

  interface Callback : Presenter.Callback<DetailState>

}
