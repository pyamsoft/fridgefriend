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
import com.pyamsoft.fridge.entry.EntryInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class CreateEntryViewModel @Inject internal constructor(
    private val interactor: EntryInteractor,
) : UiViewModel<CreateEntryViewState, CreateEntryViewEvent, CreateEntryControllerEvent>(
    CreateEntryViewState(
        name = "",
        creating = false,
        throwable = null
    )
) {

    private val commitRunner = highlander<Unit, String> { name ->
        handleCreateBegin()
        try {
            interactor.createEntry(name)
            publish(CreateEntryControllerEvent.Commit)
        } catch (throwable: Throwable) {
            throwable.onActualError {
                handleCreateError(throwable)
            }
        } finally {
            handleCreateComplete()
        }
    }

    private fun handleCreateComplete() {
        setState { copy(creating = false) }
    }

    private fun handleCreateError(throwable: Throwable) {
        setState { copy(throwable = throwable) }
    }

    private fun handleCreateBegin() {
        setState { copy(creating = true) }
    }

    override fun handleViewEvent(event: CreateEntryViewEvent) {
        return when (event) {
            is CreateEntryViewEvent.Commit -> commitNewEntry()
            is CreateEntryViewEvent.NameChanged -> updateName(event.name)
        }
    }

    private fun updateName(name: String) {
        setState { copy(name = name) }
    }

    private fun commitNewEntry() {
        viewModelScope.launch(context = Dispatchers.Default) {
            commitRunner.call(state.name)
        }
    }

}
