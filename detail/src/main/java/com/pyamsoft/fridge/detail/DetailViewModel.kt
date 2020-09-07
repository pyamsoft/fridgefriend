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
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.core.currentDate
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
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.max

class DetailViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    @Named("debug") debug: Boolean,
    entryId: FridgeEntry.Id,
    listItemPresence: FridgeItem.Presence,
    bottomOffsetBus: EventConsumer<BottomOffset>
) : BaseUpdaterViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = DetailViewState(
        entry = null,
        search = "",
        isLoading = null,
        showing = DetailViewState.Showing.FRESH,
        sort = DetailViewState.Sorts.CREATED,
        listError = null,
        undoableItem = null,
        expirationRange = null,
        isSameDayExpired = null,
        listItemPresence = listItemPresence,
        internalAllItems = emptyList(),
        items = emptyList(),
        counts = null,
        bottomOffset = 0
    ), debug = debug
) {

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
        handleListRefreshBegin()

        try {
            val items = interactor.getItems(entryId, force)
            handleListRefreshed(items)
            beginListeningForRealtime()
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
        interactor.listenForChanges(entryId) { handleRealtime(it) }
    }

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
            }
        }
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
                val isSame = interactor.isSameDayExpired()
                setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(isSame)) }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val listener = interactor.listenForExpiringSoonRangeChanged { range ->
                    setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
                }
                withContext(context = Dispatchers.Main) {
                    doOnTeardown { listener.cancel() }
                }
            }
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                val listener = interactor.listenForSameDayExpiredChanged { same ->
                    setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(same)) }
                }
                withContext(context = Dispatchers.Main) {
                    doOnTeardown { listener.cancel() }
                }
            }
        }

        doOnSaveState { outState, state ->
            outState.put(SAVED_FILTER, state.showing.name)
        }

        doOnSaveState { outState, state ->
            outState.put(SAVED_SORT, state.sort.name)
        }

        doOnSaveState { outState, state ->
            state.search.let { search ->
                if (search.isNotBlank()) {
                    outState.put(SAVED_SEARCH, search)
                    return@doOnSaveState
                }
            }

            outState.remove(SAVED_SEARCH)
        }

        doOnInit { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_FILTER) { filterName ->
                val filter = DetailViewState.Showing.valueOf(filterName)
                setState { copy(showing = filter) }
            }
        }

        doOnInit { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_SORT) { sortName ->
                val sort = DetailViewState.Sorts.valueOf(sortName)
                setState { copy(sort = sort) }
            }
        }

        doOnInit { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_SEARCH) { search ->
                setState { copy(search = search) }
            }
        }
    }

    override fun handleViewEvent(event: DetailViewEvent) {
        return when (event) {
            is DetailViewEvent.ForceRefresh -> refreshList(true)
            is DetailViewEvent.ExpandItem -> expand(event.item)
            is DetailViewEvent.ToggleArchiveVisibility -> toggleArchived()
            is DetailViewEvent.ReallyDeleteNoUndo -> setState { copy(undoableItem = null) }
            is DetailViewEvent.UndoDelete -> handleUndoDelete(event.item)
            is DetailViewEvent.AddNewItemEvent -> handleAddNew()
            is DetailViewEvent.ChangePresence -> commitPresence(event.item)
            is DetailViewEvent.Consume -> consume(event.item)
            is DetailViewEvent.Delete -> delete(event.item)
            is DetailViewEvent.Restore -> restore(event.item)
            is DetailViewEvent.Spoil -> spoil(event.item)
            is DetailViewEvent.IncreaseCount -> increaseCount(event.item)
            is DetailViewEvent.DecreaseCount -> decreaseCount(event.item)
            is DetailViewEvent.SearchQuery -> updateSearch(event.search)
            is DetailViewEvent.ChangeSort -> updateSort(event.sort)
        }
    }

    private fun handleAddNew() {
        withState {
            entry?.let {
                publish(DetailControllerEvent.AddNew(entry.id(), listItemPresence))
            }
        }
    }

    private fun updateSearch(search: String) {
        setState {
            val cleanSearch = if (search.isNotBlank()) search.trim() else ""
            val newItems = prepareListItems(internalAllItems)
            val visibleItems = getOnlyVisibleItems(newItems, search = cleanSearch)
            copy(
                internalAllItems = newItems,
                items = visibleItems,
                counts = calculateCounts(visibleItems),
                search = cleanSearch
            )
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
        if (!item.isArchived()) {
            updateItem(
                item,
                doUpdate = { interactor.commit(it) },
                onError = { handleError(it) })
        }
    }

    private fun expand(item: FridgeItem) {
        publish(ExpandForEditing(item))
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

    private fun updateSort(newSort: DetailViewState.Sorts) {
        setState { copy(sort = newSort) }
        withState {
            refreshList(false)
        }
    }

    private fun refreshList(force: Boolean) {
        viewModelScope.launch(context = Dispatchers.Default) {
            refreshRunner.call(force)
        }
    }

    private fun handleRealtime(event: FridgeItemChangeEvent) {
        return when (event) {
            is Insert -> handleRealtimeInsert(event.item)
            is Update -> handleRealtimeUpdate(event.item)
            is Delete -> handleRealtimeDelete(event.item)
        }
    }

    private fun CoroutineScope.beginListeningForRealtime() {
        launch(context = Dispatchers.Default) { realtimeRunner.call() }
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
            val newItems = internalAllItems.let { items ->
                val mutableItems = items.toMutableList()
                insertOrUpdate(mutableItems, item)
                prepareListItems(mutableItems)
            }

            val visibleItems = getOnlyVisibleItems(newItems)
            copy(
                internalAllItems = newItems,
                items = visibleItems,
                counts = calculateCounts(visibleItems)
            )
        }
    }

    private fun handleRealtimeUpdate(item: FridgeItem) {
        Timber.d("Realtime update: $item ${item.isArchived()}")
        setState {
            val list = internalAllItems
            val updatedItems = if (list.none { it.id() == item.id() }) list + item else {
                list.map { if (it.id() == item.id()) item else it }
            }
            val newItems = prepareListItems(updatedItems)
            val visibleItems = getOnlyVisibleItems(newItems)
            copy(
                internalAllItems = newItems,
                items = visibleItems,
                counts = calculateCounts(visibleItems),
                // Show undo banner if we are archiving this item, otherwise no-op
                undoableItem = if (item.isArchived()) item else undoableItem
            )
        }
    }

    private fun handleRealtimeDelete(item: FridgeItem) {
        Timber.d("Realtime delete: $item")
        setState {
            val newItems = prepareListItems(internalAllItems.filterNot { it.id() == item.id() })
            val visibleItems = getOnlyVisibleItems(newItems)
            copy(
                internalAllItems = newItems,
                items = visibleItems,
                counts = calculateCounts(visibleItems),
                // Show undo banner
                undoableItem = item
            )
        }
    }

    private fun handleListRefreshBegin() {
        setState {
            copy(isLoading = DetailViewState.Loading(true))
        }
    }

    private fun handleListRefreshed(items: List<FridgeItem>) {
        setState {
            val newItems = prepareListItems(items)
            val visibleItems = getOnlyVisibleItems(newItems)
            copy(
                internalAllItems = newItems,
                items = visibleItems,
                counts = calculateCounts(visibleItems),
                listError = null
            )
        }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState {
            copy(
                internalAllItems = emptyList(),
                items = emptyList(),
                counts = null,
                listError = throwable
            )
        }
    }

    private fun handleListRefreshComplete() {
        setState {
            copy(isLoading = DetailViewState.Loading(false))
        }
    }

    private fun handleError(throwable: Throwable) {
        setState { copy(listError = throwable) }
    }

    private fun commitPresence(item: FridgeItem) {
        changePresence(item, item.presence().flip())
    }

    private fun changePresence(oldItem: FridgeItem, newPresence: FridgeItem.Presence) {
        val item = oldItem.presence(newPresence)
        val updated = item.run {
            val dateOfPurchase = purchaseTime()
            if (newPresence == FridgeItem.Presence.HAVE) {
                if (dateOfPurchase == null) {
                    val now = currentDate()
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

        updateItem(updated, doUpdate = { interactor.commit(it) }, onError = { handleError(it) })
    }

    private fun updateItem(
        item: FridgeItem,
        doUpdate: suspend (item: FridgeItem) -> Unit,
        onError: (throwable: Throwable) -> Unit
    ) {
        require(item.isReal()) { "Commit called on a non-real item: $item" }
        update(item, doUpdate = doUpdate, onError = onError)
    }

    private fun consume(item: FridgeItem) {
        updateItem(
            item,
            doUpdate = { interactor.commit(it.consume(currentDate())) },
            onError = { handleError(it) })
    }

    private fun restore(item: FridgeItem) {
        updateItem(
            item,
            doUpdate = { interactor.commit(it.invalidateConsumption().invalidateSpoiled()) },
            onError = { handleError(it) })
    }

    private fun spoil(item: FridgeItem) {
        updateItem(
            item,
            doUpdate = { interactor.commit(it.spoil(currentDate())) },
            onError = { handleError(it) })
    }

    private fun delete(item: FridgeItem) {
        updateItem(
            item,
            doUpdate = { interactor.delete(it) },
            onError = { handleError(it) })
    }

    @CheckResult
    private fun DetailViewState.prepareListItems(items: List<FridgeItem>): List<FridgeItem> {
        val dateSorter: Comparator<FridgeItem> = Comparator { o1, o2 ->
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
        items: List<FridgeItem> = this.internalAllItems,
        search: String = this.search
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

    companion object {
        private const val SAVED_SORT = "sort"
        private const val SAVED_FILTER = "filter"
        private const val SAVED_SEARCH = "search"
    }
}
