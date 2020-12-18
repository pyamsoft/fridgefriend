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

package com.pyamsoft.fridge.detail.expand.move

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryListStateModel
import com.pyamsoft.fridge.entry.EntryViewEvent
import com.pyamsoft.fridge.entry.EntryViewState
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiViewModel
import timber.log.Timber
import javax.inject.Inject

class ItemMoveListViewModel @Inject internal constructor(
    private val delegate: EntryListStateModel,
) : UiViewModel<EntryViewState, EntryViewEvent, ItemMoveListControllerEvent>(
    initialState = delegate.initialState
) {

    init {
        val job = delegate.bind(Renderable { state ->
            state.render(viewModelScope) { newState -> setState { newState } }
        })
        doOnCleared { job.cancel() }
        doOnCleared { delegate.clear() }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is EntryViewEvent.SelectEntry -> selectEntry(event.entry)
            is EntryViewEvent.SearchQuery -> delegate.updateSearch(event.search)
            is EntryViewEvent.ChangeSort -> delegate.changeSort(event.sort)
            is EntryViewEvent.ForceRefresh -> delegate.refreshList(true)
            is EntryViewEvent.AddNew -> notHandled(event)
            is EntryViewEvent.DeleteEntry -> notHandled(event)
            is EntryViewEvent.ReallyDeleteEntryNoUndo -> notHandled(event)
            is EntryViewEvent.UndoDeleteEntry -> notHandled(event)
        }
    }

    private fun notHandled(event: EntryViewEvent) {
        throw IllegalStateException("Event not handled: $event")
    }

    private fun selectEntry(entry: FridgeEntry) {
        Timber.d("Selected entry $entry")
        publish(ItemMoveListControllerEvent.SelectedTarget(entry))
    }
}
