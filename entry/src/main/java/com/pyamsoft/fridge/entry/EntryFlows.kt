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
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent

data class EntryViewState internal constructor(
    // All currently displayed list entries
    val displayedEntries: List<EntryGroup>,
    // All the list entries before filtering
    internal val allEntries: List<EntryGroup>,
    val undoableEntry: FridgeEntry?,
    val isLoading: Boolean,
    val error: Throwable?,
    val search: String,
    val bottomOffset: Int,
    val sort: Sorts,
) : UiToolbar.State<EntryViewState.Sorts> {

    override val toolbarSearch = search
    override val toolbarSort = sort.asToolbarSort()

    data class EntryGroup internal constructor(
        val entry: FridgeEntry,
        val items: List<FridgeItem>,
    )

    @CheckResult
    internal fun FridgeEntry.matchesQuery(query: String): Boolean {
        // Empty query always matches
        return if (query.isBlank()) true else {
            this.name().contains(query, ignoreCase = true)
        }
    }

    enum class Sorts {
        CREATED,
        NAME,
    }
}

sealed class EntryViewEvent : UiViewEvent {

    data class SelectEntry internal constructor(val entry: FridgeEntry) : EntryViewEvent()

    data class SearchQuery internal constructor(val search: String) : EntryViewEvent()

    data class DeleteEntry internal constructor(val entry: FridgeEntry) : EntryViewEvent()

    data class ReallyDeleteEntryNoUndo internal constructor(
        val entry: FridgeEntry,
    ) : EntryViewEvent()

    data class UndoDeleteEntry internal constructor(val entry: FridgeEntry) : EntryViewEvent()

    data class ChangeSort internal constructor(val sort: EntryViewState.Sorts) : EntryViewEvent()

    object ForceRefresh : EntryViewEvent()

    object AddNew : EntryViewEvent()
}

sealed class EntryControllerEvent : UiControllerEvent {

    object AddEntry : EntryControllerEvent()

    data class LoadEntry internal constructor(
        val entry: FridgeEntry,
        val presence: FridgeItem.Presence,
    ) : EntryControllerEvent()
}

