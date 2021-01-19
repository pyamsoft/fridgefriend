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

package com.pyamsoft.fridge.entry.create

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateEntryViewModel @Inject internal constructor(
    private val interactor: EntryInteractor,
    entryId: FridgeEntry.Id,
) : UiViewModel<CreateEntryViewState, CreateEntryViewEvent, CreateEntryControllerEvent>(
    CreateEntryViewState(
        entry = null,
        working = false,
        throwable = null
    )
) {

    private val commitRunner = highlander<Unit, FridgeEntry> { entry ->
        setState(stateChange = { copy(working = true) }, andThen = {
            try {
                interactor.commit(entry)
                publish(CreateEntryControllerEvent.Commit)
            } catch (throwable: Throwable) {
                throwable.onActualError {
                    handleCreateError(throwable)
                }
            } finally {
                setState { copy(working = false) }
            }
        })
    }

    private val loadRunner = highlander<FridgeEntry?, FridgeEntry.Id> { id ->
        if (id.isEmpty()) FridgeEntry.create("") else interactor.loadEntry(id)
    }

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            val entry = loadRunner.call(entryId)
            setState { copy(entry = entry) }
        }
    }

    private fun CoroutineScope.handleCreateError(throwable: Throwable) {
        setState { copy(throwable = throwable) }
    }

    override fun handleViewEvent(event: CreateEntryViewEvent) = when (event) {
        is CreateEntryViewEvent.Commit -> commitNewEntry()
        is CreateEntryViewEvent.NameChanged -> updateName(event.name)
    }

    private fun updateName(name: String) {
        requireNotNull(state.entry).let { e ->
            setState { copy(entry = e.name(name)) }
        }
    }

    private fun commitNewEntry() {
        viewModelScope.launch(context = Dispatchers.Default) {
            requireNotNull(state.entry).let { e ->
                commitRunner.call(e)
            }
        }
    }
}
