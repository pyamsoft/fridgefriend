/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.fridge.entry

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.persist.PersistentEntries
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class EntryViewModel @Inject internal constructor(
    private val persistentEntries: PersistentEntries,
    @Named("debug") debug: Boolean
) : UiViewModel<EntryViewState, EntryViewEvent, EntryControllerEvent>(
    initialState = EntryViewState(
        entries = emptyList()
    ), debug = debug
) {

    init {
        doOnInit {
            loadDefaultEntry()
        }
    }

    // NOTE(Peter): This exists only as a workaround for now since we always load one default entry.
    // When we support multiple entries, this can be removed
    private fun loadDefaultEntry() {
        Timber.d("Loading default entry page")
        viewModelScope.launch {
            val entry = persistentEntries.getPersistentEntry()
            setState { copy(entries = listOf(entry)) }
            loadEntry(entry)
        }
    }

    override fun handleViewEvent(event: EntryViewEvent) {
        return when (event) {
            is EntryViewEvent.SelectEntry -> select(event.position)
        }
    }

    private fun select(position: Int) {
        withState {
            entries.getOrNull(position)?.let { entry ->
                loadEntry(entry)
            }
        }
    }

    private fun loadEntry(entry: FridgeEntry) {
        Timber.d("Loading entry page: $entry")
        publish(EntryControllerEvent.LoadEntry(entry))
    }
}
