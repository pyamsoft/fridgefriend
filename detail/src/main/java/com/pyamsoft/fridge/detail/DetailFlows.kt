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
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

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
) : UiViewState {

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

    object AddNew : DetailViewEvent()

    data class PresenceSwitched internal constructor(
        val presence: FridgeItem.Presence,
    ) : DetailViewEvent()

    data class SearchQuery internal constructor(val search: String) : DetailViewEvent()

    object ForceRefresh : DetailViewEvent()

    object ToggleArchiveVisibility : DetailViewEvent()

    object Back : DetailViewEvent()

    object ClearListError : DetailViewEvent()

    data class ChangeSort internal constructor(val sort: DetailViewState.Sorts) : DetailViewEvent()

    data class UndoDeleteItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ReallyDeleteItemNoUndo internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ConsumeItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class DeleteItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class RestoreItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class SpoilItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ExpandItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ChangeItemPresence internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class IncreaseItemCount internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class DecreaseItemCount internal constructor(val item: FridgeItem) : DetailViewEvent()
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
