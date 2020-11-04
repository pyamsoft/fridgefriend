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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class EntryViewState internal constructor(
    // All currently displayed list entries
    val displayedEntries: List<FridgeEntry>,
    // All the list entries before filtering
    internal val allEntries: List<FridgeEntry>,
    val isLoading: Boolean,
    val error: Throwable?,
    val search: String,
    val bottomOffset: Int,
) : UiViewState {

    @CheckResult
    internal fun FridgeEntry.matchesQuery(query: String): Boolean {
        // Empty query always matches
        return if (query.isBlank()) true else {
            this.name().contains(query, ignoreCase = true)
        }
    }
}

sealed class EntryViewEvent : UiViewEvent {

    data class SelectEntry internal constructor(val entry: FridgeEntry) : EntryViewEvent()

    data class SearchQuery internal constructor(val search: String) : EntryViewEvent()

    object AddNew : EntryViewEvent()

    object ForceRefresh : EntryViewEvent()
}

sealed class EntryControllerEvent : UiControllerEvent {

    data class LoadEntry internal constructor(
        val entry: FridgeEntry,
        val presence: FridgeItem.Presence
    ) : EntryControllerEvent()

    object AddEntry : EntryControllerEvent()
}
