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

package com.pyamsoft.fridge.entry.create

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.ResultWrapper
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class CreateEntryViewModel
@Inject
internal constructor(
    private val interactor: EntryInteractor,
    entryId: FridgeEntry.Id,
) :
    UiViewModel<CreateEntryViewState, CreateEntryControllerEvent>(
        CreateEntryViewState(entry = null, working = false, throwable = null)) {

  private val commitRunner =
      highlander<ResultWrapper<Boolean>, FridgeEntry> { entry -> interactor.commit(entry) }

  private val loadRunner =
      highlander<ResultWrapper<FridgeEntry>, FridgeEntry.Id> { id ->
        if (id.isEmpty()) ResultWrapper.success(FridgeEntry.create(""))
        else interactor.loadEntry(id)
      }

  init {
    viewModelScope.launch(context = Dispatchers.Default) {
      loadRunner
          .call(entryId)
          .onSuccess { setState { copy(entry = it) } }
          .onFailure { Timber.e(it, "Error loading entry: $entryId") }
          .onFailure { setState { copy(throwable = it) } }
    }
  }

  private fun CoroutineScope.handleCreateError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }

  fun handleUpdateName(name: String) {
    requireNotNull(state.entry).let { e -> setState { copy(entry = e.name(name)) } }
  }

  fun handleCommitNewEntry() {
    viewModelScope.launch(context = Dispatchers.Default) {
      requireNotNull(state.entry).let { e ->
        setState(
            stateChange = { copy(working = true) },
            andThen = {
              commitRunner
                  .call(e)
                  .onFailure { Timber.e(it, "Error creating new entry $e") }
                  .onFailure { handleCreateError(it) }
              setState(
                  stateChange = { copy(working = false) },
                  andThen = { publish(CreateEntryControllerEvent.Commit) })
            })
      }
    }
  }
}
