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

package com.pyamsoft.fridge.entry

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class EntryViewModel @Inject internal constructor(
    private val interactor: EntryInteractor,
    bottomOffsetBus: EventConsumer<BottomOffset>,
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    EntryViewState(
        isLoading = false,
        error = null,
        displayedEntries = emptyList(),
        allEntries = emptyList(),
        search = "",
        bottomOffset = 0,
    )
) {

    private val refreshRunner = highlander<Unit, Boolean> { force ->
        handleListRefreshBegin()

        try {
            val entries = interactor.loadEntries(force)
            handleListRefreshed(entries)
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error refreshing entry list")
                handleListRefreshError(e)
            }
        } finally {
            handleListRefreshComplete()
        }
    }

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            interactor.listenForChanges { handleRealtime(it) }
        }

        // Refresh each time we have UI
        refreshList(false)
    }

    private fun refreshList(force: Boolean) {
        viewModelScope.launch(context = Dispatchers.Default) {
            refreshRunner.call(force)
        }
    }

    @CheckResult
    private fun EntryViewState.regenerateEntries(entries: List<FridgeEntry>): EntryViewState {
        val newItems = prepareListEntries(entries)
        val visibleEntries = getOnlyVisibleEntries(newItems, search)
        return copy(
            allEntries = newItems,
            displayedEntries = visibleEntries,
        )
    }

    @CheckResult
    private fun EntryViewState.getOnlyVisibleEntries(
        entries: List<FridgeEntry>,
        search: String
    ): List<FridgeEntry> {
        return entries
            .asSequence()
            .filter { it.matchesQuery(search) }
            .toList()
    }


    private fun handleListRefreshBegin() {
        setState { copy(isLoading = true) }
    }

    private fun handleListRefreshed(entries: List<FridgeEntry>) {
        setState { regenerateEntries(entries).copy(error = null) }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState {
            copy(
                allEntries = emptyList(),
                displayedEntries = emptyList(),
                error = throwable
            )
        }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = false) }
    }

    private fun handleRealtime(event: FridgeEntryChangeEvent) {
        return when (event) {
            is FridgeEntryChangeEvent.Insert -> handleRealtimeInsert(event.entry)
            is FridgeEntryChangeEvent.Update -> handleRealtimeUpdate(event.entry)
            is FridgeEntryChangeEvent.Delete -> handleRealtimeDelete(event.entry)
            is FridgeEntryChangeEvent.DeleteAll -> handleRealtimeDeleteAll()
        }
    }

    private fun handleRealtimeDeleteAll() {
        Timber.d("Realtime DELETE ALL")
        setState {
            copy(allEntries = emptyList(), displayedEntries = emptyList())
        }
    }

    private fun insertOrUpdate(
        items: MutableList<FridgeEntry>,
        item: FridgeEntry
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
        items: List<FridgeEntry>,
        item: FridgeEntry
    ): Boolean {
        return items.any { item.id() == it.id() }
    }

    private fun handleRealtimeInsert(entry: FridgeEntry) {
        setState {
            val newEntries = allEntries.toMutableList().also { insertOrUpdate(it, entry) }
            regenerateEntries(newEntries)
        }
    }

    private fun handleRealtimeUpdate(entry: FridgeEntry) {
        Timber.d("Realtime update: $entry")
        setState {
            val newEntries = allEntries.toMutableList().also { insertOrUpdate(it, entry) }
            regenerateEntries(newEntries)
        }
    }

    private fun handleRealtimeDelete(entry: FridgeEntry) {
        Timber.d("Realtime delete: $entry")
        setState {
            val newEntries = allEntries.filterNot { it.id() == entry.id() }
            regenerateEntries(newEntries)
        }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is EntryViewEvent.SelectEntry -> select(event.entry)
            is EntryViewEvent.AddNew -> handleAddNew()
            is EntryViewEvent.ForceRefresh -> refreshList(true)
            is EntryViewEvent.SearchQuery -> updateSearch(event.search)
        }
    }

    private fun updateSearch(search: String) {
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

    private fun handleAddNew() {
        Timber.d("Add new entry")
        publish(EntryControllerEvent.AddEntry)
    }

    private fun select(entry: FridgeEntry) {
        Timber.d("Loading entry page: $entry")
        publish(EntryControllerEvent.LoadEntry(entry, FridgeItem.Presence.NEED))
    }

    @CheckResult
    private fun prepareListEntries(entries: List<FridgeEntry>): List<FridgeEntry> {
        return entries
            .filter { it.isReal() }
            .toList()
    }
}
