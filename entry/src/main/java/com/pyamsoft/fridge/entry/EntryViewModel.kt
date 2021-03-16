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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiViewModel
import timber.log.Timber
import javax.inject.Inject

class EntryViewModel @Inject internal constructor(
    private val delegate: EntryListStateModel,
) : UiViewModel<EntryViewState, EntryControllerEvent>(
    initialState = delegate.initialState
) {

    init {
        val scope = viewModelScope
        val job = delegate.bindState(scope, Renderable { state ->
            state.render(scope) { scope.setState { it } }
        })
        doOnCleared { job.cancel() }
        doOnCleared { delegate.clear() }

        delegate.initialize(scope)
    }

    fun handleChangeSort(sort: EntryViewState.Sorts) {
        delegate.handleChangeSort(viewModelScope, sort)
    }

    fun handleUpdateSearch(search: String) {
        delegate.handleUpdateSearch(viewModelScope, search)
    }

    fun handleUndoDelete() {
        delegate.handleUndoDelete(viewModelScope)
    }

    fun handleDeleteForever() {
        delegate.handleDeleteForever(viewModelScope)
    }

    fun handleRefresh() {
        delegate.handleRefreshList(viewModelScope, true)
    }

    fun handleDelete(index: Int) {
        delegate.handleDeleteEntry(viewModelScope, index)
    }

    private fun withEntryAt(index: Int, block: (FridgeEntry) -> Unit) {
        block(state.displayedEntries[index].entry)
    }

    fun handleSelect(index: Int) {
        withEntryAt(index) { entry ->
            Timber.d("Loading entry page: $entry")
            publish(EntryControllerEvent.LoadEntry(entry))
        }
    }

    fun handleEdit(index: Int) {
        withEntryAt(index) { entry ->
            Timber.d("Editing entry: $entry")
            publish(EntryControllerEvent.EditEntry(entry))
        }
    }

}
