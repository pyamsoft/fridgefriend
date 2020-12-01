/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.detail.expand

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
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
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

class ExpandItemViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    defaultPresence: Presence,
    dateSelectBus: EventBus<DateSelectPayload>,
    realtime: FridgeItemRealtime,
    possibleItemId: FridgeItem.Id,
    itemEntryId: FridgeEntry.Id,
) : BaseUpdaterViewModel<ExpandItemViewState, ExpandedItemViewEvent, ExpandItemControllerEvent>(
    ExpandItemViewState(
        item = null,
        throwable = null,
        sameNamedItems = emptyList(),
        similarItems = emptyList(),
        categories = emptyList()
    )
) {

    private val updateDelegate = createUpdateDelegate(interactor) { handleError(it) }

    private val itemResolveRunner = highlander<Unit, FridgeItem.Id> { resolveItemId ->
        val item = interactor.resolveItem(
            resolveItemId,
            itemEntryId,
            defaultPresence,
            force = false
        )

        setState { copy(item = item) }
    }

    init {
        doOnCleared {
            updateDelegate.teardown()
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            val categories = interactor.loadAllCategories()
            setState { copy(categories = listOf(FridgeCategory.empty()) + categories) }
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

        doOnRestoreState { savedInstanceState ->
            // Resolve the existing item id
            savedInstanceState.use(CREATED_ITEM_ID, possibleItemId.id) { savedId ->
                val resolveItemId = FridgeItem.Id(savedId)
                viewModelScope.launch(context = Dispatchers.Default) {
                    itemResolveRunner.call(resolveItemId)
                }
            }
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            realtime.listenForChanges(itemEntryId) { handleRealtimeEvent(it) }
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            dateSelectBus.onEvent { event ->
                requireNotNull(state.item).let { item ->
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

    private fun handleRealtimeEvent(event: FridgeItemChangeEvent) {
        return when (event) {
            is Update -> handleModelUpdate(event.item)
            is Insert -> handleModelUpdate(event.item)
            is Delete -> closeItem(event.item)
        }
    }

    private fun closeSelf() {
        closeItem(requireNotNull(state.item))
    }

    private fun closeItem(closeMe: FridgeItem) {
        requireNotNull(state.item).let { item ->
            if (closeMe.id() == item.id() && closeMe.entryId() == item.entryId()) {
                viewModelScope.launch(context = Dispatchers.Default) {
                    publish(ExpandItemControllerEvent.CloseExpand)
                }
            }
        }
    }

    private fun handleModelUpdate(newItem: FridgeItem) {
        requireNotNull(state.item).let { item ->
            if (item.id() == newItem.id() && item.entryId() == newItem.entryId()) {
                setState(
                    stateChange = { copy(item = newItem) },
                    andThen = { newState ->
                        val currentItem = requireNotNull(newState.item)
                        if (currentItem.isConsumed() || currentItem.isSpoiled()) {
                            Timber.d("Close item since it has been consumed/spoiled")
                            closeItem(currentItem)
                        }
                    }
                )
            }
        }
    }

    private fun pickDate() {
        val item = requireNotNull(state.item)
        if (item.isArchived()) {
            return
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

    @CheckResult
    private fun isReadyToBeReal(item: FridgeItem): Boolean {
        return isNameValid(item.name())
    }

    private fun commitCount(count: Int) {
        requireNotNull(state.item).let { item ->
            if (count > 0) {
                setFixMessage("")
                commitItem(item.count(count))
            } else {
                Timber.w("Invalid count: $count")
                handleInvalidCount(count)
            }
        }
    }

    private fun commitName(name: String) {
        requireNotNull(state.item).let { item ->
            if (isNameValid(name)) {
                setFixMessage("")
                commitItem(item.name(name))
            } else {
                Timber.w("Invalid name: $name")
                handleInvalidName(name)
            }
        }
    }

    private fun commitCategory(index: Int) {
        state.apply {
            requireNotNull(item).let { item ->
                if (item.isArchived()) {
                    return
                }

                val category = categories.getOrNull(index)
                if (category != null) {
                    val existingCategoryId = item.categoryId()
                    if (existingCategoryId != null && existingCategoryId == category.id()) {
                        Timber.d("Clearing category id")
                        commitItem(item.invalidateCategoryId())
                    } else {
                        Timber.d("Attempt save category: $category")
                        commitItem(item.categoryId(category.id()))
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
        requireNotNull(state.item).let { item ->
            Timber.d("Attempt save time: $year/${month + 1}/$day")
            val newTime = today()
                .apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                .time
            Timber.d("Save expire time: $newTime")
            commitItem(item.expireTime(newTime))
        }
    }

    private fun commitPresence() {
        requireNotNull(state.item).let { item ->
            val newItem = item.presence(item.presence().flip())
            commitItem(newItem)
        }
    }

    private fun commitItem(item: FridgeItem) {
        updateItem(item)
        findSameNamedItems(item)
        findSimilarItems(item)
    }

    private fun findSameNamedItems(item: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Default) {
            val sameNamedItems = interactor.findSameNamedItems(item)
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

    private fun updateItem(item: FridgeItem) {
        val real = item.isReal() || isReadyToBeReal(item)
        if (!real) {
            Timber.w("Commit called on a non-real item: $item")
            // Don't beacon the update anywhere since we don't want the UI to respond
            // But, commit the changes as a potential update
            handleModelUpdate(item)
            return
        }

        updateDelegate.updateItem(item)
    }

    private fun consumeSelf() {
        updateDelegate.consumeItem(requireNotNull(state.item))
    }

    private fun restoreSelf() {
        updateDelegate.restoreItem(requireNotNull(state.item))
    }

    private fun spoilSelf() {
        updateDelegate.spoilItem(requireNotNull(state.item))
    }

    private fun deleteSelf() {
        updateDelegate.deleteItem(requireNotNull(state.item))
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

    private data class FixMessageThrowable(
        override val message: String
    ) : IllegalStateException(message)
}
