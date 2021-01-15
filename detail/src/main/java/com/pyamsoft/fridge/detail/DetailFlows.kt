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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent

data class DetailViewState internal constructor(
    val isLoading: Boolean,
    // All currently displayed list items
    val displayedItems: List<FridgeItem>,
    // All the list items before filtering
    internal val allItems: List<FridgeItem>,
    val search: String,
    val entry: FridgeEntry?,
    val sort: Sorts,
    val showing: Showing,
    val listError: Throwable?,
    val undoableItem: FridgeItem?,
    val expirationRange: ExpirationRange?,
    val isSameDayExpired: IsSameDayExpired?,
    val listItemPresence: FridgeItem.Presence,
    val counts: Counts?,
    val bottomOffset: Int,
) : UiToolbar.State<DetailViewState.Sorts> {

    override val toolbarSearch = search
    override val toolbarSort = sort.asToolbarSort()

    data class Counts internal constructor(
        val totalCount: Int,
        val firstCount: Int,
        val secondCount: Int,
        val thirdCount: Int,
    )

    @CheckResult
    internal fun FridgeItem.matchesQuery(query: String): Boolean {
        // Empty query always matches
        return if (query.isBlank()) true else {
            this.name().contains(query, ignoreCase = true)
        }
    }

    enum class Showing {
        FRESH,
        CONSUMED,
        SPOILED
    }

    enum class Sorts {
        CREATED,
        NAME,
        PURCHASED,
        EXPIRATION
    }

    @CheckResult
    internal fun filterValid(items: List<FridgeItem>): Sequence<FridgeItem> {
        return items.asSequence().filterNot { it.isEmpty() }
    }

    data class ExpirationRange internal constructor(val range: Int)

    data class IsSameDayExpired internal constructor(val isSame: Boolean)
}

sealed class DetailViewEvent : UiViewEvent {

    sealed class Main : DetailViewEvent() {

        sealed class AddEvent : Main() {

            object AddNew : AddEvent()

            object ToggleArchiveVisibility : AddEvent()

            object ClearListError : AddEvent()

            data class UndoDeleteItem internal constructor(val item: FridgeItem) : AddEvent()

            data class ReallyDeleteItemNoUndo internal constructor(val item: FridgeItem) :
                AddEvent()

        }

        sealed class ListEvent : Main() {

            object ForceRefresh : ListEvent()

            data class IncreaseItemCount internal constructor(val index: Int) : ListEvent()

            data class DecreaseItemCount internal constructor(val index: Int) : ListEvent()

            data class ConsumeItem internal constructor(val index: Int) : ListEvent()

            data class DeleteItem internal constructor(val index: Int) : ListEvent()

            data class RestoreItem internal constructor(val index: Int) : ListEvent()

            data class SpoilItem internal constructor(val index: Int) : ListEvent()

            data class ExpandItem internal constructor(val index: Int) : ListEvent()

            data class ChangeItemPresence internal constructor(val index: Int) : ListEvent()

        }

        sealed class ToolbarEvent : Main() {

            data class SearchQuery internal constructor(val search: String) : ToolbarEvent()

            object Back : ToolbarEvent()

            data class ChangeSort internal constructor(
                val sort: DetailViewState.Sorts
            ) : ToolbarEvent()

        }

    }

    sealed class SwitcherEvent : DetailViewEvent() {

        data class PresenceSwitched internal constructor(
            val presence: FridgeItem.Presence,
        ) : SwitcherEvent()
    }

}

sealed class DetailControllerEvent : UiControllerEvent {

    data class AddNew internal constructor(
        val id: FridgeEntry.Id,
        val presence: FridgeItem.Presence,
    ) : DetailControllerEvent()

    data class ExpandForEditing internal constructor(
        val item: FridgeItem,
    ) : DetailControllerEvent()

    object EntryArchived : DetailControllerEvent()

    object Back : DetailControllerEvent()
}
