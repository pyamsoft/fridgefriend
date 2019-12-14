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
import com.pyamsoft.fridge.detail.ExpandVisibilityEvent
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.fridge.detail.item.isNameValid
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
    dateSelectBus: EventBus<DateSelectPayload>,
    expandVisibilityBus: EventBus<ExpandVisibilityEvent>,
    realtime: FridgeItemRealtime,
    private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
    private val interactor: DetailInteractor
) : BaseUpdaterViewModel<ExpandItemViewState, ExpandedItemViewEvent, ExpandItemControllerEvent>(
    initialState = ExpandItemViewState(
        item = item.presence(defaultPresence),
        throwable = null,
        sameNamedItems = emptyList(),
        similarItems = emptyList()
    )
) {

    private val itemEntryId = item.entryId()
    private val itemId = item.id()

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                realtime.listenForChanges(itemEntryId)
                    .onEvent { handleRealtimeEvent(it) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                fakeRealtime.onEvent { handleRealtimeEvent(it) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
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

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                expandVisibilityBus.send(ExpandVisibilityEvent(true))
            }
        }

        doOnTeardown {
            // Don't use coroutine scope because launch will insta-die on teardown
            expandVisibilityBus.publish(ExpandVisibilityEvent(false))
        }
    }

    override fun handleViewEvent(event: ExpandedItemViewEvent) {
        return when (event) {
            is ExpandedItemViewEvent.CommitName -> commitName(event.name)
            is ExpandedItemViewEvent.CommitCount -> commitCount(event.count)
            is ExpandedItemViewEvent.CommitPresence -> commitPresence()
            is ExpandedItemViewEvent.PickDate -> pickDate()
            is ExpandedItemViewEvent.CloseItem -> closeSelf()
            is ExpandedItemViewEvent.DeleteItem -> deleteSelf()
            is ExpandedItemViewEvent.ConsumeItem -> consumeSelf()
            is ExpandedItemViewEvent.SpoilItem -> spoilSelf()
        }
    }

    private fun consumeSelf() {
        withState {
            update(item, doUpdate = { interactor.consume(it) }, onError = { handleError(it) }) {
                closeItem(it)
            }
        }
    }

    private fun spoilSelf() {
        withState {
            update(item, doUpdate = { interactor.spoil(it) }, onError = { handleError(it) }) {
                closeItem(it)
            }
        }
    }

    private fun deleteSelf() {
        withState {
            update(item, doUpdate = { interactor.delete(it) }, onError = { handleError(it) }) {
                closeItem(it)
            }
        }
    }

    private fun handleRealtimeEvent(event: FridgeItemChangeEvent) {
        return when (event) {
            is Update -> handleModelUpdate(event.item)
            is Insert -> handleModelUpdate(event.item)
            is Delete -> closeItem(event.item)
        }
    }

    private fun closeSelf() {
        withState {
            closeItem(item)
        }
    }

    private fun closeItem(item: FridgeItem) {
        if (itemId == item.id() && itemEntryId == item.entryId()) {
            publish(ExpandItemControllerEvent.CloseExpand)
        }
    }

    private fun handleModelUpdate(newItem: FridgeItem) {
        if (itemId == newItem.id() && itemEntryId == newItem.entryId()) {
            setState { copy(item = newItem) }
        }
    }

    private fun pickDate() {
        withState {
            val expireTime = item.expireTime()
            val month: Int
            val day: Int
            val year: Int

            if (expireTime != null) {
                val date = Calendar.getInstance()
                    .apply { time = expireTime }

                // Month is zero indexed in storage
                month = date.get(Calendar.MONTH)
                day = date.get(Calendar.DAY_OF_MONTH)
                year = date.get(Calendar.YEAR)
            } else {
                month = 0
                day = 0
                year = 0
            }

            Timber.d("Launch date picker from date: $year ${month + 1} $day")
            publish(ExpandItemControllerEvent.DatePick(item, year, month, day))
        }
    }

    @CheckResult
    private fun isReadyToBeReal(item: FridgeItem): Boolean {
        return isNameValid(item.name())
    }

    private fun commitCount(count: Int) {
        withState {
            item.let { item ->
                if (count > 0) {
                    setFixMessage("")
                    commitItem(item.count(count), item.presence())
                } else {
                    Timber.w("Invalid count: $count")
                    handleInvalidCount(count)
                }
            }
        }
    }

    private fun commitName(name: String) {
        withState {
            item.let { item ->
                if (isNameValid(name)) {
                    setFixMessage("")
                    commitItem(item.name(name), item.presence())
                } else {
                    Timber.w("Invalid name: $name")
                    handleInvalidName(name)
                }
            }
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
        commitItem(oldItem.expireTime(newTime), oldItem.presence())
    }

    private fun commitPresence() {
        withState {
            item.let { item ->
                val oldPresence = item.presence()
                val newItem = item.presence(oldPresence.flip())
                commitItem(newItem, oldPresence)
            }
        }
    }

    private fun commitItem(item: FridgeItem, oldPresence: Presence) {
        updateItem(item)
        findSameNamedItems(item, oldPresence)
        findSimilarItems(item)
    }

    private fun findSameNamedItems(item: FridgeItem, oldPresence: Presence) {
        if (oldPresence != NEED) {
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

    private fun updateItem(oldItem: FridgeItem) {
        val real = oldItem.isReal() || isReadyToBeReal(oldItem)
        if (!real) {
            Timber.w("Commit called on a non-real item: $oldItem, fake callback")
            handleFakeCommit(oldItem)
            return
        }

        val item = oldItem.run {
            val dateOfPurchase = purchaseTime()
            if (presence() == HAVE) {
                if (dateOfPurchase == null) {
                    val now = Date()
                    Timber.d("${oldItem.name()} purchased! $now")
                    return@run purchaseTime(now)
                }
            } else {
                if (dateOfPurchase != null) {
                    Timber.d("${oldItem.name()} purchase date cleared")
                    return@run invalidatePurchase()
                }
            }

            return@run this
        }

        viewModelScope.launch(context = Dispatchers.Main) {
            update(item, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
        }
    }

    private fun handleFakeCommit(item: FridgeItem) {
        viewModelScope.launch {
            Timber.w("Not ready to commit item yet: $item")
            fakeRealtime.send(Insert(item))
        }
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
}
