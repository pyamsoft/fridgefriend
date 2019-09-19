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

package com.pyamsoft.fridge.detail.expand

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.fridge.detail.item.DateSelectPayload
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.CloseExpand
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CloseItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitCount
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ConsumeItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.DeleteItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.PickDate
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.SpoilItem
import com.pyamsoft.fridge.detail.item.DetailItemViewModel
import com.pyamsoft.fridge.detail.item.isNameValid
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class ExpandItemViewModel @Inject internal constructor(
    item: FridgeItem,
    defaultPresence: Presence,
    fakeRealtime: EventBus<FridgeItemChangeEvent>,
    dateSelectBus: EventBus<DateSelectPayload>,
    realtime: FridgeItemRealtime,
    private val interactor: DetailInteractor
) : DetailItemViewModel(item = item.presence(defaultPresence), fakeRealtime = fakeRealtime) {

    private val updateRunner = highlander<Unit, FridgeItem> { item ->
        try {
            interactor.commit(item.makeReal())
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error updating item: ${item.id()}")
                handleError(e)
            }
        }
    }

    private val itemEntryId = item.entryId()
    private val itemId = item.id()

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                launch {
                    realtime.listenForChanges(itemEntryId)
                        .onEvent { handleRealtimeEvent(it) }
                }

            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                launch { fakeRealtime.onEvent { handleRealtimeEvent(it) } }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                launch {
                    dateSelectBus.onEvent { event ->
                        if (event.oldItem.entryId() != itemEntryId) {
                            return@onEvent
                        }

                        if (event.oldItem.id() != itemId) {
                            return@onEvent
                        }

                        commitDate(event.oldItem, event.year, event.month, event.day)
                    }
                }
            }
        }
    }

    override fun handleViewEvent(event: DetailItemViewEvent) {
        return when (event) {
            is CommitName -> commitName(event.oldItem, event.name)
            is CommitCount -> commitCount(event.oldItem, event.count)
            is CommitPresence -> commitPresence(event.oldItem, event.presence)
            is ExpandItem -> expandItem(event.item)
            is PickDate -> pickDate(event.oldItem, event.year, event.month, event.day)
            is CloseItem -> closeSelf(event.item)
            is DeleteItem -> deleteSelf(event.item)
            is ConsumeItem -> consumeSelf(event.item)
            is SpoilItem -> spoilSelf(event.item)
        }
    }

    private fun consumeSelf(item: FridgeItem) {
        remove(item, doRemove = { interactor.consume(it) }) { closeSelf(it) }
    }

    private fun spoilSelf(item: FridgeItem) {
        remove(item, doRemove = { interactor.spoil(it) }) { closeSelf(it) }
    }

    private fun deleteSelf(item: FridgeItem) {
        remove(item, doRemove = { interactor.delete(it) }) { closeSelf(it) }
    }

    private fun handleRealtimeEvent(event: FridgeItemChangeEvent) {
        return when (event) {
            is Update -> handleModelUpdate(event.item)
            is Insert -> handleModelUpdate(event.item)
            is Delete -> closeSelf(event.item)
        }
    }

    private fun closeSelf(newItem: FridgeItem) {
        if (itemId == newItem.id() && itemEntryId == newItem.entryId()) {
            publish(CloseExpand)
        }
    }

    private fun handleModelUpdate(newItem: FridgeItem) {
        if (itemId == newItem.id() && itemEntryId == newItem.entryId()) {
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

    private fun commitCount(
        oldItem: FridgeItem,
        count: Int
    ) {
        if (count > 0) {
            setFixMessage("")
            commitItem(item = oldItem.count(count))
        } else {
            Timber.w("Invalid count: $count")
            handleInvalidCount(count)
        }
    }

    private fun commitName(
        oldItem: FridgeItem,
        name: String
    ) {
        if (isNameValid(name)) {
            setFixMessage("")
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
        commitItem(item = oldItem.presence(presence))
    }

    private fun commitItem(item: FridgeItem) {
        updateItem(item)
        findSameNamedItems(item)
        findSimilarItems(item)
    }

    private fun findSameNamedItems(item: FridgeItem) {
        if (item.presence() != NEED) {
            return
        }

        viewModelScope.launch(context = Dispatchers.Main) {
            val sameNamedItems = interactor.findSameNamedItems(item.name(), HAVE)
            setState { copy(sameNamedItems = sameNamedItems) }
        }
    }

    private fun findSimilarItems(item: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Main) {
            val similarItems = interactor.findSimilarNamedItems(item)
            setState { copy(similarItems = similarItems) }
        }
    }

    private fun updateItem(item: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Main) {
            if (item.isReal() || isReadyToBeReal(item)) {
                updateRunner.call(item.run {
                    val dateOfPurchase = purchaseTime()
                    if (presence() == HAVE) {
                        if (dateOfPurchase == null) {
                            val now = Date()
                            Timber.d("${item.name()} purchased! $now")
                            return@run purchaseTime(now)
                        }
                    } else {
                        if (dateOfPurchase != null) {
                            Timber.d("${item.name()} purchase date cleared")
                            return@run invalidatePurchase()
                        }
                    }

                    return@run this
                })
            } else {
                Timber.w("Commit called on a non-real item: $item, fake callback")
                handleFakeCommit(item)
            }
        }
    }

    private suspend fun handleFakeCommit(item: FridgeItem) {
        fakeRealtime.send(Insert(item))
        Timber.w("Not ready to commit item yet: $item")
    }

    private fun handleInvalidName(name: String) {
        setFixMessage("ERROR: Name $name is invalid. Please fix.")
    }

    private fun handleInvalidCount(count: Int) {
        setFixMessage("ERROR: Count $count is invalid. Please fix.")
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(throwable = throwable) }
    }

    private fun setFixMessage(message: String) {
        setState {
            copy(throwable = if (message.isBlank()) null else IllegalArgumentException(message))
        }
    }

    private fun expandItem(item: FridgeItem) {
        publish(ExpandDetails(item))
    }
}
