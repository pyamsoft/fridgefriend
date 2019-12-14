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
import com.pyamsoft.fridge.core.Preferences
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.DetailViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.DetailViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.DetailViewEvent.ReallyDeleteNoUndo
import com.pyamsoft.fridge.detail.DetailViewEvent.ToggleArchiveVisibility
import com.pyamsoft.fridge.detail.DetailViewEvent.UndoDelete
import com.pyamsoft.fridge.detail.DetailViewState.Loading
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

class DetailViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
    private val expandVisibilityBus: EventBus<ExpandVisibilityEvent>,
    listItemPresence: FridgeItem.Presence,
    entry: FridgeEntry
) : BaseUpdaterViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = DetailViewState(
        isLoading = null,
        items = null,
        showArchived = false,
        listError = null,
        undoableItem = null,
        actionVisible = null,
        isExpanded = null,
        expirationRange = interactor.getExpiringSoonRange(),
        isSameDayExpired = interactor.isSameDayExpired(),
        listItemPresence = listItemPresence
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

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                expandVisibilityBus.onEvent {
                    setState { copy(isExpanded = DetailViewState.Expanded(it.visible)) }
                }
            }
        }

        var expiringSoonUnregister: Preferences.Unregister? = null

        doOnInit {
            expiringSoonUnregister = interactor.watchForExpiringSoonChanges { newRange ->
                setState { copy(expirationRange = newRange) }
            }
        }

        doOnTeardown {
            expiringSoonUnregister?.unregister()
            expiringSoonUnregister = null
        }

        var isSameDayExpiredUnregister: Preferences.Unregister? = null

        doOnInit {
            isSameDayExpiredUnregister = interactor.watchForSameDayExpiredChange { newSameDay ->
                setState { copy(isSameDayExpired = newSameDay) }
            }
        }

        doOnTeardown {
            isSameDayExpiredUnregister?.unregister()
            isSameDayExpiredUnregister = null
        }
    }

    override fun handleViewEvent(event: DetailViewEvent) {
        return when (event) {
            is ForceRefresh -> refreshList(true)
            is ExpandItem -> expand(event.index)
            is ToggleArchiveVisibility -> toggleArchived()
            is ReallyDeleteNoUndo -> setState { copy(undoableItem = null) }
            is UndoDelete -> handleUndoDelete(event.item)
            is DetailViewEvent.ScrollActionVisibilityChange -> changeActionVisibility(event.visible)
            is DetailViewEvent.AddNewItemEvent -> publish(DetailControllerEvent.AddNew(entryId))
            is DetailViewEvent.DoneScrollActionVisibilityChange -> doneChangingActionVisibility()
            is DetailViewEvent.ChangePresence -> commitPresence(event.index)
            is DetailViewEvent.Consume -> consume(event.index)
            is DetailViewEvent.Delete -> delete(event.index)
            is DetailViewEvent.Restore -> restore(event.index)
            is DetailViewEvent.Spoil -> spoil(event.index)
        }
    }

    private fun expand(index: Int) {
        withItem(index) { item ->
            publish(ExpandForEditing(item))
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

    private fun toggleArchived() {
        setState { copy(showArchived = !showArchived) }
        refreshList(false)
    }

    @CheckResult
    private fun getListItems(
        showArchived: Boolean,
        items: List<FridgeItem>,
        listItemPresence: FridgeItem.Presence
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
                items = items.orEmpty().let { list ->
                    val newItems = list.toMutableList()
                    insert(newItems, item)
                    return@let getListItems(showArchived, newItems, listItemPresence)
                })
        }
    }

    private fun handleRealtimeUpdate(item: FridgeItem) {
        // Remove Archived items
        if (item.isArchived()) {
            setState {
                copy(
                    items = getListItems(
                        showArchived,
                        items.orEmpty().filterNot { it.id() == item.id() }, listItemPresence
                    ),
                    undoableItem = item
                )
            }
        } else {
            setState {
                val oldList = items.orEmpty()
                copy(
                    items = getListItems(showArchived,
                        if (oldList.map { it.id() }.contains(item.id())) {
                            oldList.map { old ->
                                if (old.id() == item.id()) {
                                    return@map item
                                } else {
                                    return@map old
                                }
                            }
                        } else {
                            oldList + item
                        }
                        , listItemPresence)
                )
            }
        }
    }

    private fun handleRealtimeDelete(item: FridgeItem) {
        setState {
            copy(
                items = getListItems(
                    showArchived,
                    items.orEmpty().filterNot { it.id() == item.id() }, listItemPresence
                ),
                undoableItem = item
            )
        }
    }

    private fun handleListRefreshBegin() {
        setState { copy(isLoading = Loading(true)) }
    }

    private fun handleListRefreshed(items: List<FridgeItem>) {
        setState {
            copy(
                items = getListItems(showArchived, items, listItemPresence),
                listError = null
            )
        }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState { copy(items = emptyList(), listError = throwable) }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = Loading(false)) }
    }

    private fun handleFakeDelete(item: FridgeItem) {
        viewModelScope.launch {
            Timber.w("Remove called on a non-real item: $item, fake callback")
            fakeRealtime.send(Delete(item))
        }
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(listError = throwable) }
    }

    private fun commitPresence(index: Int) {
        withItem(index) { item ->
            if (item.isReal()) {
                changePresence(item, item.presence().flip())
            }
        }
    }

    private fun changePresence(oldItem: FridgeItem, newPresence: FridgeItem.Presence) {
        if (!oldItem.isReal()) {
            Timber.w("Cannot commit change for not-real item: $oldItem")
            return
        }

        val item = oldItem.presence(newPresence)
        val updated = item.run {
            val dateOfPurchase = purchaseTime()
            if (newPresence == FridgeItem.Presence.HAVE) {
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
        }

        viewModelScope.launch {
            update(updated, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
        }
    }

    private inline fun withItem(index: Int, crossinline func: (item: FridgeItem) -> Unit) {
        withState {
            items?.getOrNull(index)?.let { func(it) }
        }
    }

    private fun consume(index: Int) {
        withItem(index) { item ->
            if (item.isReal()) {
                update(item, doUpdate = { interactor.consume(it) }, onError = { handleError(it) })
            }
        }
    }

    private fun restore(index: Int) {
        withItem(index) { item ->
            if (item.isReal()) {
                update(item, doUpdate = { interactor.restore(it) }, onError = { handleError(it) })
            }
        }
    }

    private fun spoil(index: Int) {
        withItem(index) { item ->
            if (item.isReal()) {
                update(item, doUpdate = { interactor.spoil(it) }, onError = { handleError(it) })
            }
        }
    }

    private fun delete(index: Int) {
        withItem(index) { item ->
            if (item.isReal()) {
                update(item, doUpdate = { interactor.delete(it) }, onError = { handleError(it) })
            } else {
                handleFakeDelete(item)
            }
        }
    }
}
