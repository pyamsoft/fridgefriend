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
    internal fun FridgeEntry.matchesQuery(query: String, defaultValue: Boolean): Boolean {
        // Empty query always matches
        return if (query.isBlank()) defaultValue else {
            this.name().contains(query, ignoreCase = true)
        }
    }

    enum class Sorts {
        CREATED,
        NAME,
    }
}

sealed class ReadOnlyListEvents : UiViewEvent {

    data class Select internal constructor(val index: Int) : ReadOnlyListEvents()

    object ForceRefresh : ReadOnlyListEvents()
}

sealed class EntryViewEvent : UiViewEvent {

    sealed class ListEvents : EntryViewEvent() {

        data class EditEntry internal constructor(val index: Int) : ListEvents()

        data class SelectEntry internal constructor(val index: Int) : ListEvents()

        data class DeleteEntry internal constructor(val index: Int) : ListEvents()

        object ForceRefresh : ListEvents()
    }

    sealed class AddEvent : EntryViewEvent() {

        object ReallyDeleteEntryNoUndo : AddEvent()

        object UndoDeleteEntry : AddEvent()

        object AddNew : AddEvent()

    }

    sealed class ToolbarEvent : EntryViewEvent() {

        data class SearchQuery(val search: String) : ToolbarEvent()

        data class ChangeSort(val sort: EntryViewState.Sorts) : ToolbarEvent()

    }
}

sealed class EntryControllerEvent : UiControllerEvent {

    data class LoadEntry internal constructor(
        val entry: FridgeEntry
    ) : EntryControllerEvent()

    data class EditEntry internal constructor(
        val entry: FridgeEntry
    ) : EntryControllerEvent()

}

