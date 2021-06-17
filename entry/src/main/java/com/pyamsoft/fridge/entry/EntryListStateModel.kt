/*
 * Copyright 2021 Peter Kenji Yamanaka
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
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiStateModel
import com.pyamsoft.pydroid.arch.onActualError
import com.pyamsoft.pydroid.bus.EventConsumer
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// Share this single StateModel between the entire fragment scope
// Used particularly for ItemMove module
@FragmentScope
class EntryListStateModel
@Inject
constructor(
    private val interactor: EntryInteractor,
    private val bottomOffsetBus: EventConsumer<BottomOffset>,
) :
    UiStateModel<EntryViewState>(
        EntryViewState(
            isLoading = false,
            error = null,
            displayedEntries = emptyList(),
            allEntries = emptyList(),
            search = "",
            bottomOffset = 0,
            undoableEntry = null,
            sort = EntryViewState.Sorts.CREATED)) {

  private val refreshRunner =
      highlander<Unit, Boolean> { force ->
        try {
          val groups =
              interactor.loadEntries(force).map { entry ->
                val items = interactor.loadItems(force, entry)
                return@map EntryViewState.EntryGroup(entry = entry, items = items)
              }
          handleListRefreshed(groups)
        } catch (error: Throwable) {
          error.onActualError { e ->
            Timber.e(e, "Error refreshing entry list")
            handleListRefreshError(e)
          }
        }
      }

  private val undoRunner =
      highlander<Unit, FridgeEntry> { entry ->
        try {
          require(entry.isReal()) { "Cannot undo for non-real entry: $entry" }
          interactor.commit(entry)
        } catch (error: Throwable) {
          error.onActualError { e -> Timber.e(e, "Error undoing entry: ${entry.id()}") }
        }
      }

  internal fun initialize(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
    }

    scope.launch(context = Dispatchers.Default) {
      interactor.listenForEntryChanges { handleEventRealtime(it) }
    }

    scope.launch(context = Dispatchers.Default) {
      interactor.listenForItemChanges { handleItemRealtime(it) }
    }

    handleRefreshList(scope, false)
  }

  @CheckResult
  private fun EntryViewState.regenerateEntries(
      entries: List<EntryViewState.EntryGroup>
  ): EntryViewState {
    val newEntries = prepareListEntries(entries)
    val visibleEntries = getOnlyVisibleEntries(newEntries, search)
    return copy(
        allEntries = newEntries,
        displayedEntries = visibleEntries,
    )
  }

  @CheckResult
  private fun EntryViewState.getOnlyVisibleEntries(
      entries: List<EntryViewState.EntryGroup>,
      search: String,
  ): List<EntryViewState.EntryGroup> {
    return entries.asSequence().filter { it.entry.matchesQuery(search, true) }.toList()
  }

  private fun CoroutineScope.handleListRefreshed(entries: List<EntryViewState.EntryGroup>) {
    setState { regenerateEntries(entries) }
  }

  private fun CoroutineScope.handleListRefreshError(throwable: Throwable) {
    setState { copy(error = throwable) }
  }

  private fun CoroutineScope.handleEventRealtime(event: FridgeEntryChangeEvent) =
      when (event) {
        is FridgeEntryChangeEvent.DeleteAll -> handleRealtimeEntryDeleteAll()
        is FridgeEntryChangeEvent.Insert -> handleRealtimeEntryInsert(event.entry)
        is FridgeEntryChangeEvent.Update -> handleRealtimeEntryUpdate(event.entry)
        is FridgeEntryChangeEvent.Delete -> handleRealtimeEntryDelete(event.entry, event.offerUndo)
      }

  private fun CoroutineScope.handleItemRealtime(event: FridgeItemChangeEvent) =
      when (event) {
        is FridgeItemChangeEvent.Insert -> handleRealtimeItemInsert(event.item)
        is FridgeItemChangeEvent.Update -> handleRealtimeItemUpdate(event.item)
        is FridgeItemChangeEvent.Delete -> handleRealtimeItemDelete(event.item)
      }

  private fun CoroutineScope.handleRealtimeItemInsert(item: FridgeItem) {
    setState {
      val newEntries =
          allEntries.map { group ->
            if (group.entry.id() != item.entryId()) group
            else {
              group.copy(items = group.items + item)
            }
          }
      regenerateEntries(newEntries)
    }
  }

  private fun CoroutineScope.handleRealtimeItemUpdate(item: FridgeItem) {
    setState {
      val newEntries =
          allEntries.map { group ->
            if (group.entry.id() != item.entryId()) group
            else {
              group.copy(items = group.items.map { if (it.id() == item.id()) item else it })
            }
          }
      regenerateEntries(newEntries)
    }
  }

  private fun CoroutineScope.handleRealtimeItemDelete(item: FridgeItem) {
    setState {
      val newEntries =
          allEntries.map { group ->
            if (group.entry.id() != item.entryId()) group
            else {
              group.copy(items = group.items.filterNot { it.id() == item.id() })
            }
          }
      regenerateEntries(newEntries)
    }
  }

  private fun CoroutineScope.handleRealtimeEntryDeleteAll() {
    Timber.d("Realtime DELETE ALL")
    setState { copy(allEntries = emptyList(), displayedEntries = emptyList()) }
  }

  private fun CoroutineScope.handleRealtimeEntryInsert(entry: FridgeEntry) {
    setState {
      val newEntries = allEntries + EntryViewState.EntryGroup(entry = entry, items = emptyList())
      regenerateEntries(newEntries)
    }
  }

  private fun CoroutineScope.handleRealtimeEntryUpdate(entry: FridgeEntry) {
    Timber.d("Realtime update: $entry")
    setState {
      val newEntries =
          allEntries.map { group ->
            if (group.entry.id() != entry.id()) group
            else {
              group.copy(entry = entry)
            }
          }
      regenerateEntries(newEntries)
    }
  }

  private fun CoroutineScope.handleRealtimeEntryDelete(entry: FridgeEntry, offerUndo: Boolean) {
    Timber.d("Realtime delete: $entry")
    setState {
      val newEntries = allEntries.filterNot { it.entry.id() == entry.id() }
      regenerateEntries(newEntries)
          .copy(
              // Show undo banner
              undoableEntry = if (offerUndo) entry else null)
    }
  }

  @CheckResult
  private fun EntryViewState.prepareListEntries(
      entries: List<EntryViewState.EntryGroup>,
  ): List<EntryViewState.EntryGroup> {
    val dateSorter =
        Comparator<EntryViewState.EntryGroup> { o1, o2 ->
          when (sort) {
            EntryViewState.Sorts.CREATED -> o1.entry.createdTime().compareTo(o2.entry.createdTime())
            EntryViewState.Sorts.NAME ->
                o1.entry.name().compareTo(o2.entry.name(), ignoreCase = true)
          }
        }

    return entries
        .filter { it.entry.isReal() }
        .sortedWith(dateSorter)
        .map { group ->
          return@map group.copy(
              items = group.items.filterNot { it.isArchived() }.filterNot { it.isReal() })
        }
        .toList()
  }

  fun handleUpdateSearch(scope: CoroutineScope, search: String) {
    scope.setState(
        stateChange = {
          val cleanSearch = if (search.isNotBlank()) search.trim() else ""
          copy(search = cleanSearch)
        },
        andThen = { handleRefreshList(this, false) })
  }

  fun handleRefreshList(scope: CoroutineScope, force: Boolean) {
    scope.launch(context = Dispatchers.Default) {
      setState(
          stateChange = { copy(isLoading = true) },
          andThen = {
            refreshRunner.call(force)
            setState { copy(isLoading = false) }
          })
    }
  }

  private inline fun withEntryAt(index: Int, block: (FridgeEntry) -> Unit) {
    block(state.displayedEntries[index].entry)
  }

  internal fun handleDeleteEntry(scope: CoroutineScope, index: Int) {
    scope.launch(context = Dispatchers.Default) {
      withEntryAt(index) { interactor.delete(it, true) }
    }
  }

  internal fun handleDeleteForever(scope: CoroutineScope) {
    scope.setState { copy(undoableEntry = null) }
  }

  internal fun handleUndoDelete(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      undoRunner.call(requireNotNull(state.undoableEntry))
    }
  }

  fun handleChangeSort(scope: CoroutineScope, newSort: EntryViewState.Sorts) {
    scope.setState(
        stateChange = { copy(sort = newSort) }, andThen = { handleRefreshList(this, false) })
  }
}
