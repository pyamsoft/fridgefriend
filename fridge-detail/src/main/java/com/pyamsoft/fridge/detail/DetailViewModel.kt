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
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.fridge.detail.expand.ItemExpandPayload
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

class DetailViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    private val itemExpandedBus: EventBus<ItemExpandPayload>,
    @Named("entry_id") private val entryId: FridgeEntry.Id,
    @Named("debug") debug: Boolean,
    listItemPresence: FridgeItem.Presence
) : BaseUpdaterViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = DetailViewState(
        entry = null,
        isLoading = null,
        showing = DetailViewState.Showing.FRESH,
        sort = DetailViewState.Sorts.CREATED,
        listError = null,
        undoableItem = null,
        expirationRange = null,
        isSameDayExpired = null,
        listItemPresence = listItemPresence,
        isItemExpanded = false,
        items = emptyList()
    ), debug = debug
) {

    private val undoRunner = highlander<Unit, FridgeItem> { item ->
        try {
            val undoneItem = item.invalidateSpoiled().invalidateConsumption()
            if (undoneItem.isReal()) {
                interactor.commit(undoneItem)
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
        interactor.listenForChanges(entryId)
            .onEvent { handleRealtime(it) }
    }

    init {
        doOnInit {
            refreshList(false)
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val entry = interactor.loadEntry(entryId)
                setState { copy(entry = entry) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val range = interactor.getExpiringSoonRange()
                setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val expiringSoonUnregister = interactor.watchForExpiringSoonChanges { range ->
                    setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
                }
                doOnTeardown {
                    expiringSoonUnregister.unregister()
                }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val isSame = interactor.isSameDayExpired()
                setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(isSame)) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val isSameDayExpiredUnregister = interactor.watchForSameDayExpiredChange { same ->
                    setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(same)) }
                }
                doOnTeardown {
                    isSameDayExpiredUnregister.unregister()
                }
            }
        }

        doOnInit {
            itemExpandedBus.scopedEvent(Dispatchers.Default) {
                setState { copy(isItemExpanded = it.expanded) }
            }
        }
    }

    override fun handleViewEvent(event: DetailViewEvent) {
        return when (event) {
            is DetailViewEvent.ForceRefresh -> refreshList(true)
            is DetailViewEvent.ExpandItem -> expand(event.item)
            is DetailViewEvent.ToggleSort -> toggleSort()
            is DetailViewEvent.ToggleArchiveVisibility -> toggleArchived()
            is DetailViewEvent.ReallyDeleteNoUndo -> setState { copy(undoableItem = null) }
            is DetailViewEvent.UndoDelete -> handleUndoDelete(event.item)
            is DetailViewEvent.AddNewItemEvent -> publish(DetailControllerEvent.AddNew(entryId))
            is DetailViewEvent.ChangePresence -> commitPresence(event.item)
            is DetailViewEvent.Consume -> consume(event.item)
            is DetailViewEvent.Delete -> delete(event.item)
            is DetailViewEvent.Restore -> restore(event.item)
            is DetailViewEvent.Spoil -> spoil(event.item)
            is DetailViewEvent.IncreaseCount -> increaseCount(event.item)
            is DetailViewEvent.DecreaseCount -> decreaseCount(event.item)
        }
    }

    private fun decreaseCount(item: FridgeItem) {
        val newCount = item.count() - 1
        val newItem = item.count(max(1, newCount))
        updateCount(newItem)
        if (newCount <= 0 && newItem.presence() == FridgeItem.Presence.HAVE) {
            viewModelScope.launch(context = Dispatchers.Default) {
                if (interactor.isZeroCountConsideredConsumed()) {
                    consume(newItem)
                }
            }
        }
    }

    private fun increaseCount(item: FridgeItem) {
        updateCount(item.count(item.count() + 1))
    }

    private fun updateCount(item: FridgeItem) {
        if (item.isReal()) {
            update(item, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
        }
    }

    private fun expand(item: FridgeItem) {
        withState {
            if (showing == DetailViewState.Showing.FRESH) {
                publish(ExpandForEditing(item))
            }
        }
    }

    private fun handleUndoDelete(item: FridgeItem) {
        viewModelScope.launch(context = Dispatchers.Default) { undoRunner.call(item) }
    }

    private fun toggleArchived() {
        setState {
            val newShowing = when (showing) {
                DetailViewState.Showing.FRESH -> DetailViewState.Showing.CONSUMED
                DetailViewState.Showing.CONSUMED -> DetailViewState.Showing.SPOILED
                DetailViewState.Showing.SPOILED -> DetailViewState.Showing.FRESH
            }
            copy(showing = newShowing)
        }
        withState {
            refreshList(false)
        }
    }

    private fun toggleSort() {
        setState {
            val expirationSortResult = if (listItemPresence == FridgeItem.Presence.HAVE) {
                DetailViewState.Sorts.PURCHASED
            } else {
                DetailViewState.Sorts.CREATED
            }

            val newSort = when (sort) {
                DetailViewState.Sorts.CREATED -> DetailViewState.Sorts.NAME
                DetailViewState.Sorts.NAME -> DetailViewState.Sorts.EXPIRATION
                DetailViewState.Sorts.EXPIRATION -> expirationSortResult
                DetailViewState.Sorts.PURCHASED -> DetailViewState.Sorts.CREATED
            }
            copy(sort = newSort)
        }
        withState {
            refreshList(false)
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

    private fun insertOrUpdate(
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
                items = items.let { items ->
                    val newItems = items.toMutableList()
                    insertOrUpdate(newItems, item)
                    prepareListItems(newItems)
                }
            )
        }
    }

    private fun handleRealtimeUpdate(item: FridgeItem) {
        setState {
            copy(
                items = prepareListItems(if (items.map { it.id() }.contains(item.id())) {
                    items.map { old ->
                        if (old.id() == item.id()) {
                            return@map item
                        } else {
                            return@map old
                        }
                    }
                } else {
                    items + item
                }),

                // Show undo banner if we are archiving this item, otherwise no-op
                undoableItem = if (item.isArchived()) item else undoableItem
            )
        }
    }

    private fun handleRealtimeDelete(item: FridgeItem) {
        setState {
            copy(
                items = prepareListItems(items.filterNot { it.id() == item.id() }),
                undoableItem = item
            )
        }
    }

    private fun handleListRefreshBegin() {
        setState { copy(isLoading = DetailViewState.Loading(true)) }
    }

    private fun handleListRefreshed(items: List<FridgeItem>) {
        setState {
            copy(
                items = prepareListItems(items),
                listError = null
            )
        }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState {
            copy(
                items = emptyList(),
                listError = throwable
            )
        }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = DetailViewState.Loading(false)) }
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(listError = throwable) }
    }

    private fun commitPresence(item: FridgeItem) {
        if (item.isReal()) {
            changePresence(item, item.presence().flip())
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

    private fun consume(item: FridgeItem) {
        if (item.isReal()) {
            update(
                item,
                doUpdate = { interactor.consume(it) },
                onError = { handleError(it) })
        }
    }

    private fun restore(item: FridgeItem) {
        if (item.isReal()) {
            update(
                item,
                doUpdate = { interactor.restore(it) },
                onError = { handleError(it) })
        }
    }

    private fun spoil(item: FridgeItem) {
        if (item.isReal()) {
            update(item, doUpdate = { interactor.spoil(it) }, onError = { handleError(it) })
        }
    }

    private fun delete(item: FridgeItem) {
        if (item.isReal()) {
            update(
                item,
                doUpdate = { interactor.delete(it) },
                onError = { handleError(it) })
        }
    }

    @CheckResult
    private fun DetailViewState.prepareListItems(items: List<FridgeItem>): List<FridgeItem> {
        val listItems = filterValid { items }
            .filter { it.presence() == listItemPresence }
            .sortedWith(dateSorter)
            .toList()

        return if (listItems.isEmpty()) emptyList() else listItems.boundary()
    }

    @CheckResult
    private fun List<FridgeItem>.boundary(): List<FridgeItem> {
        return listOf(FridgeItem.empty()) + this + listOf(FridgeItem.empty())
    }
}
