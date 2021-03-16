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

package com.pyamsoft.fridge.detail.expand.move

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryListStateModel
import com.pyamsoft.fridge.entry.EntryViewState
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiViewModel
import timber.log.Timber
import javax.inject.Inject

class ItemMoveListViewModel @Inject internal constructor(
    @param:MoveInternalApi private val delegate: EntryListStateModel,
    entryId: FridgeEntry.Id,
) : UiViewModel<EntryViewState, ItemMoveListControllerEvent>(
    initialState = delegate.initialState
) {

    init {
        val scope = viewModelScope
        val job = delegate.bindState(scope, Renderable { state ->
            state.render(scope) { newState ->
                scope.setState {
                    newState.copy(
                        displayedEntries = newState.displayedEntries.filterNot {
                            it.entry.id() == entryId
                        }
                    )
                }
            }
        })
        doOnCleared { job.cancel() }
        doOnCleared { delegate.clear() }
    }

    fun handleRefreshList() {
        delegate.handleRefreshList(viewModelScope, true)
    }

    fun handleUpdateSearch(search: String) {
        delegate.handleUpdateSearch(viewModelScope, search)
    }

    fun handleUpdateSort(sort: EntryViewState.Sorts) {
        delegate.handleChangeSort(viewModelScope, sort)
    }

    private inline fun withEntryAt(index: Int, block: (FridgeEntry) -> Unit) {
        block(state.displayedEntries[index].entry)
    }

    fun handleSelectEntry(index: Int) {
        withEntryAt(index) { entry ->
            Timber.d("Selected entry $entry")
            publish(ItemMoveListControllerEvent.Selected(entry))
        }
    }
}
