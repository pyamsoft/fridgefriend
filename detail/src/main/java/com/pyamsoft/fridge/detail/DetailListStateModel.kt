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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.cleanMidnight
import com.pyamsoft.fridge.db.item.daysLaterMidnight
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.item.isExpired
import com.pyamsoft.fridge.db.item.isExpiringSoon
import com.pyamsoft.fridge.detail.base.createUpdateDelegate
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.UiStateModel
import com.pyamsoft.pydroid.arch.onActualError
import com.pyamsoft.pydroid.util.PreferenceListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.max

class DetailListStateModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    entryId: FridgeEntry.Id,
    listItemPresence: FridgeItem.Presence,
    bottomOffsetBus: EventConsumer<BottomOffset>
) : UiStateModel<DetailViewState>(
    initialState = DetailViewState(
        entry = null,
        search = "",
        isLoading = false,
        showing = DetailViewState.Showing.FRESH,
        sort = DetailViewState.Sorts.CREATED,
        listError = null,
        undoableItem = null,
        expirationRange = null,
        isSameDayExpired = null,
        listItemPresence = listItemPresence,
        counts = null,
        bottomOffset = 0,
        displayedItems = emptyList(),
        allItems = emptyList()
    )
) {

    private val updateDelegate =
        createUpdateDelegate(stateModelScope, interactor) { handleError(it) }

    private var expirationListener: PreferenceListener? = null
    private var sameDayListener: PreferenceListener? = null

    private val undoRunner = highlander<Unit, FridgeItem> { item ->
        try {
            require(item.isReal()) { "Cannot undo for non-real item: $item" }
            interactor.commit(item.invalidateSpoiled().invalidateConsumption())
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error undoing item: ${item.id()}")
            }
        }
    }

    private val refreshRunner = highlander<Unit, Boolean> { force ->
        try {
            handleListRefreshBegin()
            val items = interactor.getItems(entryId, force)
            handleListRefreshed(items)
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error refreshing item list")
                handleListRefreshError(e)
            }
        } finally {
            handleListRefreshComplete()
        }
    }

    init {
        stateModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            val entry = interactor.loadEntry(entryId)
            setState { copy(entry = entry) }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            val range = interactor.getExpiringSoonRange()
            setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            val isSame = interactor.isSameDayExpired()
            setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(isSame)) }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            expirationListener = interactor.listenForExpiringSoonRangeChanged { range ->
                setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
            }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            sameDayListener = interactor.listenForSameDayExpiredChanged { same ->
                setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(same)) }
            }
        }

        stateModelScope.launch(context = Dispatchers.Default) {
            interactor.listenForChanges(entryId) { handleRealtime(it) }
        }

        refreshList(false)
    }

    override fun clear() {
        super.clear()
        updateDelegate.teardown()

        sameDayListener?.cancel()
        sameDayListener = null

        expirationListener?.cancel()
        expirationListener = null
    }

    private fun handleRealtime(event: FridgeItemChangeEvent) {
        return when (event) {
            is Insert -> handleRealtimeInsert(event.item)
            is Update -> handleRealtimeUpdate(event.item)
            is Delete -> handleRealtimeDelete(event.item)
        }
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
        items: List<FridgeItem>,
        item: FridgeItem
    ): Boolean {
        return items.any { item.id() == it.id() && item.entryId() == it.entryId() }
    }

    private fun handleRealtimeInsert(item: FridgeItem) {
        setState {
            val newItems = allItems.toMutableList().also { insertOrUpdate(it, item) }
            regenerateItems(newItems)
        }
    }

    private fun handleRealtimeUpdate(item: FridgeItem) {
        Timber.d("Realtime update: $item ${item.isArchived()}")
        setState {
            val newItems = allItems.toMutableList().also { insertOrUpdate(it, item) }
            regenerateItems(newItems).copy(
                // Show undo banner if we are archiving this item, otherwise no-op
                undoableItem = if (item.isArchived()) item else undoableItem
            )
        }
    }

    private fun handleRealtimeDelete(item: FridgeItem) {
        Timber.d("Realtime delete: $item")
        setState {
            val newItems = allItems.filterNot { it.id() == item.id() }
            regenerateItems(newItems).copy(
                // Show undo banner
                undoableItem = item
            )
        }
    }

    @CheckResult
    private fun DetailViewState.regenerateItems(items: List<FridgeItem>): DetailViewState {
        val newItems = prepareListItems(items)
        val visibleItems = getOnlyVisibleItems(newItems, search)
        return copy(
            allItems = newItems,
            displayedItems = visibleItems,
            counts = calculateCounts(visibleItems),
        )
    }

    private fun handleListRefreshed(items: List<FridgeItem>) {
        setState {
            regenerateItems(items).copy(listError = null)
        }
    }

    private fun handleListRefreshBegin() {
        setState { copy(isLoading = true) }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState {
            copy(
                allItems = emptyList(),
                displayedItems = emptyList(),
                counts = null,
                listError = throwable
            )
        }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = false) }
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(listError = throwable) }
    }

    @CheckResult
    private fun DetailViewState.prepareListItems(items: List<FridgeItem>): List<FridgeItem> {
        val dateSorter = Comparator<FridgeItem> { o1, o2 ->
            when (sort) {
                DetailViewState.Sorts.CREATED -> o1.createdTime().compareTo(o2.createdTime())
                DetailViewState.Sorts.NAME -> o1.name().compareTo(o2.name(), ignoreCase = true)
                DetailViewState.Sorts.PURCHASED -> o1.purchaseTime().compareTo(o2.purchaseTime())
                DetailViewState.Sorts.EXPIRATION -> o1.expireTime().compareTo(o2.expireTime())
            }
        }

        return filterValid(items)
            .filter { it.presence() == listItemPresence }
            .sortedWith(dateSorter)
            .toList()
    }

    // Compare dates which may be null
    // Null dates come after non-null dates
    @CheckResult
    private fun Date?.compareTo(other: Date?): Int {
        return if (this == null && other == null) 0 else {
            if (other == null) -1 else this?.compareTo(other) ?: 1
        }
    }

    @CheckResult
    private fun DetailViewState.calculateCounts(items: List<FridgeItem>): DetailViewState.Counts? {
        return when (showing) {
            DetailViewState.Showing.FRESH -> calculateFreshCounts(items)
            DetailViewState.Showing.CONSUMED -> null
            DetailViewState.Showing.SPOILED -> null
        }
    }

    @CheckResult
    private fun DetailViewState.calculateFreshCounts(items: List<FridgeItem>): DetailViewState.Counts? {
        return when (listItemPresence) {
            FridgeItem.Presence.HAVE -> {
                val expiringSoonRange = this.expirationRange?.range ?: return null
                val isSameDayExpired = this.isSameDayExpired?.isSame ?: return null
                val today = today().cleanMidnight()
                val later = today().daysLaterMidnight(expiringSoonRange)
                generateHaveFreshCount(items, today, later, isSameDayExpired)
            }
            FridgeItem.Presence.NEED -> generateNeedFreshCount(items)
        }
    }

    @CheckResult
    private fun DetailViewState.generateNeedFreshCount(items: List<FridgeItem>): DetailViewState.Counts {
        val validItems = filterValid(items)
            .filterNot { it.isArchived() }

        val totalCount = validItems.sumBy { it.count() }
        return DetailViewState.Counts(
            totalCount = totalCount,
            firstCount = 0,
            secondCount = 0,
            thirdCount = 0
        )
    }

    @CheckResult
    private fun DetailViewState.generateHaveFreshCount(
        items: List<FridgeItem>,
        today: Calendar,
        later: Calendar,
        isSameDayExpired: Boolean
    ): DetailViewState.Counts {
        val validItems = filterValid(items)
            .filterNot { it.isArchived() }

        val totalCount = validItems.sumBy { it.count() }

        val expiringSoonItemCount = validItems
            .filter { it.isExpiringSoon(today, later, isSameDayExpired) }
            .sumBy { it.count() }

        val expiredItemCount = validItems
            .filter { it.isExpired(today, isSameDayExpired) }
            .sumBy { it.count() }

        val freshItemCount = totalCount - expiringSoonItemCount - expiredItemCount

        return DetailViewState.Counts(
            totalCount = totalCount,
            firstCount = freshItemCount,
            secondCount = expiringSoonItemCount,
            thirdCount = expiredItemCount
        )
    }

    @CheckResult
    private fun DetailViewState.getOnlyVisibleItems(
        items: List<FridgeItem>,
        search: String
    ): List<FridgeItem> {
        return items
            .asSequence()
            .filter {
                return@filter when (showing) {
                    DetailViewState.Showing.FRESH -> !it.isArchived()
                    DetailViewState.Showing.CONSUMED -> it.isConsumed()
                    DetailViewState.Showing.SPOILED -> it.isSpoiled()
                }
            }.filter { it.matchesQuery(search) }
            .toList()
    }

    internal fun deleteForever(item: FridgeItem) {
        val undoable = state.undoableItem
        if (undoable?.id() == item.id()) {
            setState { copy(undoableItem = null) }
        }
    }

    internal fun handlePresenceSwitch(presence: FridgeItem.Presence) {
        setState(
            stateChange = {
                copy(
                    listItemPresence = presence,
                    // Reset the showing
                    showing = DetailViewState.Showing.FRESH,
                    // Reset the sort
                    sort = DetailViewState.Sorts.CREATED
                )
            },
            andThen = {
                refreshList(false)
            }
        )
    }

    internal fun updateSearch(search: String) {
        setState(
            stateChange = {
                val cleanSearch = if (search.isNotBlank()) search.trim() else ""
                copy(search = cleanSearch)
            },
            andThen = {
                refreshList(false)
            }
        )
    }

    internal fun decreaseCount(item: FridgeItem) {
        val newCount = item.count() - 1
        val newItem = item.count(max(1, newCount))
        updateCount(newItem)
        if (newCount <= 0 && newItem.presence() == FridgeItem.Presence.HAVE) {
            stateModelScope.launch(context = Dispatchers.Default) {
                if (interactor.isZeroCountConsideredConsumed()) {
                    consume(newItem)
                }
            }
        }
    }

    internal fun increaseCount(item: FridgeItem) {
        updateCount(item.count(item.count() + 1))
    }

    private fun updateCount(item: FridgeItem) {
        if (!item.isArchived()) {
            updateDelegate.updateItem(item)
        }
    }

    internal fun handleUndoDelete(item: FridgeItem) {
        val undoable = state.undoableItem
        if (undoable?.id() == item.id()) {
            stateModelScope.launch(context = Dispatchers.Default) {
                undoRunner.call(item)
            }
        }
    }

    internal fun toggleArchived() {
        setState(
            stateChange = {
                val newShowing = when (showing) {
                    DetailViewState.Showing.FRESH -> DetailViewState.Showing.CONSUMED
                    DetailViewState.Showing.CONSUMED -> DetailViewState.Showing.SPOILED
                    DetailViewState.Showing.SPOILED -> DetailViewState.Showing.FRESH
                }
                copy(showing = newShowing)
            },
            andThen = {
                refreshList(false)
            }
        )
    }

    internal fun updateSort(newSort: DetailViewState.Sorts) {
        setState(stateChange = { copy(sort = newSort) }, andThen = { refreshList(false) })
    }

    internal fun refreshList(force: Boolean) {
        stateModelScope.launch(context = Dispatchers.Default) {
            refreshRunner.call(force)
        }
    }

    internal fun commitPresence(item: FridgeItem) {
        changePresence(item, item.presence().flip())
    }

    internal fun changePresence(oldItem: FridgeItem, newPresence: FridgeItem.Presence) {
        updateDelegate.updateItem(oldItem.presence(newPresence))
    }

    internal fun consume(item: FridgeItem) {
        updateDelegate.consumeItem(item)
    }

    internal fun restore(item: FridgeItem) {
        updateDelegate.restoreItem(item)
    }

    internal fun spoil(item: FridgeItem) {
        updateDelegate.spoilItem(item)
    }

    internal fun delete(item: FridgeItem) {
        updateDelegate.deleteItem(item)
    }

    internal fun updateFilter(showing: DetailViewState.Showing) {
        setState { copy(showing = showing) }
    }
}