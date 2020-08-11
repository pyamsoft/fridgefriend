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
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.fridge.detail.expand.date.DateSelectPayload
import com.pyamsoft.fridge.detail.item.isNameValid
import com.pyamsoft.pydroid.arch.EventBus
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class ExpandItemViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    defaultPresence: Presence,
    dateSelectBus: EventBus<DateSelectPayload>,
    realtime: FridgeItemRealtime,
    possibleItemId: FridgeItem.Id,
    itemEntryId: FridgeEntry.Id,
    @Named("debug") debug: Boolean
) : BaseUpdaterViewModel<ExpandItemViewState, ExpandedItemViewEvent, ExpandItemControllerEvent>(
    initialState = ExpandItemViewState(
        item = null,
        throwable = null,
        sameNamedItems = emptyList(),
        similarItems = emptyList(),
        categories = emptyList()
    ), debug = debug
) {

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val categories = interactor.loadAllCategories()
                setState { copy(categories = listOf(FridgeCategory.empty()) + categories) }
            }
        }

        doOnSaveState { outState, state ->
            // If this viewmodel lives on a "new" item which has since been created
            if (possibleItemId.isEmpty()) {
                // Save the newly created item id if possible
                state.item?.let { item ->
                    if (item.isReal()) {
                        outState.put(CREATED_ITEM_ID, item.id().id)
                        return@doOnSaveState
                    }
                }
            }

            outState.remove(CREATED_ITEM_ID)
        }

        doOnInit { savedInstanceState ->
            // Resolve the existing item id
            val savedId = savedInstanceState.getOrDefault(CREATED_ITEM_ID, possibleItemId.id)
            val resolveItemId = FridgeItem.Id(savedId)
            viewModelScope.launch(context = Dispatchers.Default) {
                val item = interactor.resolveItem(
                    resolveItemId,
                    itemEntryId,
                    defaultPresence,
                    force = false
                )
                setState { copy(item = item) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                realtime.listenForChanges(itemEntryId) { handleRealtimeEvent(it) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                dateSelectBus.onEvent { event ->
                    withState {
                        requireNotNull(item).let { item ->
                            if (event.entryId != item.entryId()) {
                                return@let
                            }

                            if (event.itemId != item.id()) {
                                return@let
                            }

                            commitDate(event.year, event.month, event.day)
                        }
                    }
                }
            }
        }
    }

    override fun handleViewEvent(event: ExpandedItemViewEvent) {
        return when (event) {
            is ExpandedItemViewEvent.CommitCategory -> commitCategory(event.index)
            is ExpandedItemViewEvent.CommitName -> commitName(event.name)
            is ExpandedItemViewEvent.CommitCount -> commitCount(event.count)
            is ExpandedItemViewEvent.CommitPresence -> commitPresence()
            is ExpandedItemViewEvent.PickDate -> pickDate()
            is ExpandedItemViewEvent.CloseItem -> closeSelf()
            is ExpandedItemViewEvent.DeleteItem -> deleteSelf()
            is ExpandedItemViewEvent.ConsumeItem -> consumeSelf()
            is ExpandedItemViewEvent.SpoilItem -> spoilSelf()
            is ExpandedItemViewEvent.RestoreItem -> restoreSelf()
            is ExpandedItemViewEvent.SelectSimilar -> similarSelected(event.item)
        }
    }

    private fun similarSelected(item: FridgeItem) {
        Timber.d("Selected similar item: $item")
        // TODO(Peter): Similar item selected response
        //
        // Upon selection, adopt the item.name() and figure out the difference between when the item
        // was created and the expiration date if it exists. Apply this difference to the current time for this new item to resolve
        // the expiration date
    }

    private fun consumeSelf() {
        withState {
            update(
                requireNotNull(item),
                doUpdate = { interactor.commit(it.consume(currentDate())) },
                onError = { handleError(it) }) {
                closeItem(it)
            }
        }
    }

    private fun restoreSelf() {
        withState {
            update(
                requireNotNull(item),
                doUpdate = { interactor.commit(it.invalidateConsumption().invalidateSpoiled()) },
                onError = { handleError(it) })
            // Don't close the dialog on restore so user can continue interacting.
        }
    }

    private fun spoilSelf() {
        withState {
            update(
                requireNotNull(item),
                doUpdate = { interactor.commit(it.spoil(currentDate())) },
                onError = { handleError(it) }) {
                closeItem(it)
            }
        }
    }

    private fun deleteSelf() {
        withState {
            update(
                requireNotNull(item),
                doUpdate = { interactor.delete(it) },
                onError = { handleError(it) }) {
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
            closeItem(requireNotNull(item))
        }
    }

    private fun closeItem(closeMe: FridgeItem) {
        withState {
            requireNotNull(item).let { item ->
                if (closeMe.id() == item.id() && closeMe.entryId() == item.entryId()) {
                    viewModelScope.launch(context = Dispatchers.Default) {
                        publish(ExpandItemControllerEvent.CloseExpand)
                    }
                }
            }
        }
    }

    private fun handleModelUpdate(newItem: FridgeItem) {
        withState {
            requireNotNull(item).let { item ->
                if (item.id() == newItem.id() && item.entryId() == newItem.entryId()) {
                    setState { copy(item = newItem) }
                }
            }
        }
    }

    private fun pickDate() {
        withState {
            val item = requireNotNull(item)
            if (item.isArchived()) {
                return@withState
            }

            val expireTime = item.expireTime()
            val month: Int
            val day: Int
            val year: Int

            if (expireTime != null) {
                val date = today().apply { time = expireTime }

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
            requireNotNull(item).let { item ->
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
            requireNotNull(item).let { item ->
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

    private fun commitCategory(index: Int) {
        withState {
            requireNotNull(item).let { item ->
                if (item.isArchived()) {
                    return@withState
                }

                val category = categories.getOrNull(index)
                if (category != null) {
                    val existingCategoryId = item.categoryId()
                    if (existingCategoryId != null && existingCategoryId == category.id()) {
                        Timber.d("Clearing category id")
                        commitItem(item.invalidateCategoryId(), item.presence())
                    } else {
                        Timber.d("Attempt save category: $category")
                        commitItem(item.categoryId(category.id()), item.presence())
                    }
                }
            }
        }
    }

    private fun commitDate(
        year: Int,
        month: Int,
        day: Int
    ) {
        withState {
            requireNotNull(item).let { item ->
                Timber.d("Attempt save time: $year/${month + 1}/$day")
                val newTime = today()
                    .apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month)
                        set(Calendar.DAY_OF_MONTH, day)
                    }
                    .time
                Timber.d("Save expire time: $newTime")
                commitItem(item.expireTime(newTime), item.presence())
            }
        }
    }

    private fun commitPresence() {
        withState {
            requireNotNull(item).let { item ->
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

        viewModelScope.launch(context = Dispatchers.Default) {
            val sameNamedItems = interactor.findSameNamedItems(item.name(), HAVE)
            setState { copy(sameNamedItems = sameNamedItems) }
        }
    }

    private fun findSimilarItems(rootItem: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Default) {
            val similarItems = interactor.findSimilarNamedItems(rootItem)
            setState {
                copy(similarItems = similarItems
                    // For now since we do not have the full similar item support
                    .distinctBy { it.name().toLowerCase(Locale.getDefault()).trim() }
                    .map { item ->
                        ExpandItemViewState.SimilarItem(
                            item, item.name().toLowerCase(Locale.getDefault())
                        )
                    }
                )
            }
        }
    }

    private fun updateItem(oldItem: FridgeItem) {
        val real = oldItem.isReal() || isReadyToBeReal(oldItem)
        if (!real) {
            Timber.w("Commit called on a non-real item: $oldItem")
            // Don't beacon the update anywhere since we don't want the UI to respond
            // But, commit the changes as a potential update
            handleModelUpdate(oldItem)
            return
        }

        val item = oldItem.run {
            val dateOfPurchase = purchaseTime()
            if (presence() == HAVE) {
                if (dateOfPurchase == null) {
                    val now = currentDate()
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

        update(item, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
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
            copy(throwable = if (message.isBlank()) null else FixMessageThrowable(message))
        }
    }

    companion object {

        // Item was new but was created. Then the App is killed from memory conditions.
        // Instead of reloading a new item when the app loads, reload the current newly created item
        private const val CREATED_ITEM_ID = "created_item_id"
    }

    private data class FixMessageThrowable internal constructor(
        override val message: String
    ) : IllegalStateException(message)
}