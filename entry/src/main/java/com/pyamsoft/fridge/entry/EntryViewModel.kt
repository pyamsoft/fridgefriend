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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class EntryViewModel @Inject internal constructor(
    private val interactor: EntryInteractor,
    bottomOffsetBus: EventConsumer<BottomOffset>,
    @Named("debug") debug: Boolean
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    initialState = EntryViewState(
        isLoading = false,
        error = null,
        entries = emptyList(),
        bottomOffset = 0,
    ), debug = debug
) {

    private val refreshRunner = highlander<Unit, Boolean> { force ->
        handleListRefreshBegin()

        try {
            val entries = interactor.loadEntries(force)
            handleListRefreshed(entries)
            beginListeningForRealtime()
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error refreshing entry list")
                handleListRefreshError(e)
            }
        } finally {
            handleListRefreshComplete()
        }
    }

    private val realtimeRunner = highlander<Unit> {
        interactor.listenForChanges { handleRealtime(it) }
    }

    init {
        doOnInit {
            refreshList(false)
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
            }
        }
    }

    private fun refreshList(force: Boolean) {
        viewModelScope.launch(context = Dispatchers.Default) {
            refreshRunner.call(force)
        }
    }

    private fun handleListRefreshBegin() {
        setState { copy(isLoading = true) }
    }

    private fun handleListRefreshed(entries: List<FridgeEntry>) {
        setState { copy(entries = prepareListEntries(entries), error = null) }
    }

    private fun handleListRefreshError(throwable: Throwable) {
        setState { copy(entries = emptyList(), error = throwable) }
    }

    private fun handleListRefreshComplete() {
        setState { copy(isLoading = false) }
    }

    private fun CoroutineScope.beginListeningForRealtime() {
        launch(context = Dispatchers.Default) { realtimeRunner.call() }
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
            copy(entries = emptyList())
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
            val newEntries = entries.let { entries ->
                val mutableEntries = entries.toMutableList()
                insertOrUpdate(mutableEntries, entry)
                prepareListEntries(mutableEntries)
            }

            copy(entries = newEntries)
        }
    }

    private fun handleRealtimeUpdate(entry: FridgeEntry) {
        Timber.d("Realtime update: $entry")
        setState {
            val list = entries.toMutableList()
            insertOrUpdate(list, entry)
            val newEntries = prepareListEntries(list)
            copy(entries = newEntries)
        }
    }

    private fun handleRealtimeDelete(entry: FridgeEntry) {
        Timber.d("Realtime delete: $entry")
        setState {
            val newEntries = prepareListEntries(entries.filterNot { it.id() == entry.id() })
            copy(entries = newEntries)
        }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is EntryViewEvent.SelectEntry -> select(event.position)
            is EntryViewEvent.AddNew -> handleAddNew()
        }
    }

    private fun handleAddNew() {
        // TODO(Peter) Real handler, prompt for adding new
        select(0)
    }

    private fun select(position: Int) {
        withState {
            val entry = entries[position]
            Timber.d("Loading entry page: $entry")
            publish(EntryControllerEvent.LoadEntry(entry, FridgeItem.Presence.HAVE))
        }
    }

    @CheckResult
    private fun prepareListEntries(entries: List<FridgeEntry>): List<FridgeEntry> {
        return entries
            .filter { it.isReal() }
            .toList()
    }
}
