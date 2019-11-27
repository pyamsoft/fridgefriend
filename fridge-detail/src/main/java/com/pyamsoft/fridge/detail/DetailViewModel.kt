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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.DetailControllerEvent.DatePick
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.DetailControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.DetailViewEvent.CloseEntry
import com.pyamsoft.fridge.detail.DetailViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.DetailViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.DetailViewEvent.PickDate
import com.pyamsoft.fridge.detail.DetailViewEvent.ReallyDeleteNoUndo
import com.pyamsoft.fridge.detail.DetailViewEvent.ToggleArchiveVisibility
import com.pyamsoft.fridge.detail.DetailViewEvent.UndoDelete
import com.pyamsoft.fridge.detail.DetailViewState.Loading
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class DetailViewModel @Inject internal constructor(
    interactor: DetailInteractor,
    fakeRealtime: EventBus<FridgeItemChangeEvent>,
    private val listItemPresence: FridgeItem.Presence,
    entry: FridgeEntry
) : UiViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = DetailViewState(
        isLoading = null,
        items = emptyList(),
        showArchived = false,
        listError = null,
        undoableItem = null,
        actionVisible = null
    )
) {

    private val entryId = entry.id()

    private val undoRunner = highlander<Unit, FridgeItem> { item ->
        try {
            val undoneItem = item.invalidateSpoiled().invalidateConsumption()
            if (undoneItem.isReal()) {
                interactor.commit(undoneItem)
            } else {
                fakeRealtime.send(Insert(undoneItem))
            }
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error undoing item: ${item.id()}")
            }
        }
    }

    private val refreshRunner = highlander<Unit, Boolean> { force ->
        handleListRefreshBegin()

        try {
            val items = interactor.getItems(entryId, force)
            handleListRefreshed(items)
            beginListeningForChanges()
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error refreshing item list")
                handleListRefreshError(e)
            }
        } finally {
            handleListRefreshComplete()
        }
    }

    private val realtimeRunner = highlander<Unit> {
        withContext(context = Dispatchers.Default) {
            launch {
                interactor.listenForChanges(entryId)
                    .onEvent { handleRealtime(it) }
            }

            launch {
                fakeRealtime.onEvent { handleRealtime(it) }
            }
        }
    }

    init {
        doOnInit {
            refreshList(false)
        }
    }

    override fun handleViewEvent(event: DetailViewEvent) {
        return when (event) {
            is ForceRefresh -> refreshList(true)
            is ExpandItem -> publish(ExpandForEditing(event.item))
            is PickDate -> publish(DatePick(event.oldItem, event.year, event.month, event.day))
            is CloseEntry -> publish(NavigateUp)
            is ToggleArchiveVisibility -> toggleArchived(event.show)
            is ReallyDeleteNoUndo -> setState { copy(undoableItem = null) }
            is UndoDelete -> handleUndoDelete(event.item)
            is DetailViewEvent.ScrollActionVisibilityChange -> changeActionVisibility(event.visible)
            is DetailViewEvent.AddNewItemEvent -> publish(DetailControllerEvent.AddNew(entryId))
            is DetailViewEvent.DoneScrollActionVisibilityChange -> doneChangingActionVisibility()
        }
    }

    private fun changeActionVisibility(visible: Boolean) {
        setState { copy(actionVisible = DetailViewState.ActionVisible(visible)) }
    }

    private fun doneChangingActionVisibility() {
        setState { copy(actionVisible = null) }
    }

    private fun handleUndoDelete(item: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Default) { undoRunner.call(item) }
    }

    private fun toggleArchived(show: Boolean) {
        setState { copy(showArchived = !show) }
        refreshList(false)
    }

    @CheckResult
    private fun getListItems(
        showArchived: Boolean,
        items: List<FridgeItem>
    ): List<FridgeItem> {
        val listItems = items
            .asSequence()
            .sortedWith(Comparator { o1, o2 ->
                return@Comparator when {
                    o1.isArchived() && o2.isArchived() -> 0
                    o1.isArchived() -> 1
                    o2.isArchived() -> -1
                    else -> 0
                }
            })
            .filter { if (showArchived) it.isArchived() else !it.isArchived() }
            .filter { it.presence() == listItemPresence }
            .toList()

        return when {
            listItems.isEmpty() -> listItems
            listItems.first().id().isNotBlank() -> listOf(FridgeItem.empty(entryId)) + listItems
            else -> listItems
        }
    }

    private fun refreshList(force: Boolean) {
        viewModelScope.launch { refreshRunner.call(force) }
    }

    private fun handleRealtime(event: FridgeItemChangeEvent) {
        return when (event) {
            is Insert -> handleRealtimeInsert(event.item)
            is Update -> handleRealtimeUpdate(event.item)
            is Delete -> handleRealtimeDelete(event.item)
        }
    }

    @CheckResult
    private fun CoroutineScope.beginListeningForChanges() = launch(context = Dispatchers.Default) {
        realtimeRunner.call()
    }

    private fun insert(
        items: MutableList<FridgeItem>,
        item: FridgeItem
    ) {
        if (!checkExists(items, item)) {
            items.add(item)
        } else {
            for ((index, oldItem) in items.withIndex()) {
                if (oldItem.id() == item.id()) {
                    items[index] = item
                    break
                }
            }
        }
    }

    @CheckResult
    private fun checkExists(
        items: MutableList<FridgeItem>,
        item: FridgeItem
    ): Boolean {
        return items.any { item.id() == it.id() && item.entryId() == it.entryId() }
    }

    private fun handleRealtimeInsert(item: FridgeItem) {
        setState {
            copy(
                items = items.let { list ->
                    val newItems = list.toMutableList()
                    insert(newItems, item)
                    return@let getListItems(showArchived, newItems)
                })
        }
    }

    private fun handleRealtimeUpdate(item: FridgeItem) {
        // Remove Archived items
        if (item.isArchived()) {
            setState {
                copy(
                    items = getListItems(showArchived, items.filterNot { it.id() == item.id() }),
                    undoableItem = item
                )
            }
        } else {
            setState {
                copy(
                    items = getListItems(showArchived,
                        if (items.map { it.id() }.contains(item.id())) {
                            items.map { old ->
                                if (old.id() == item.id()) {
                                    return@map item
                                } else {
                                    return@map old
                                }
                            }
                        } else {
                            items + item
                        }
                    )
                )
            }
        }
    }

    private fun handleRealtimeDelete(item: FridgeItem) {
        setState {
            copy(
                items = getListItems(showArchived, items.filterNot { it.id() == item.id() }),
                undoableItem = item
            )
        }
    }

    private fun handleListRefreshBegin() {
        setState { copy(isLoading = Loading(true)) }
    }

    private fun handleListRefreshed(items: List<FridgeItem>) {
        setState { copy(items = getListItems(showArchived, items), listError = null) }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState { copy(items = emptyList(), listError = throwable) }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = Loading(false)) }
    }
}
