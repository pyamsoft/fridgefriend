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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import java.util.Date

data class DetailViewState(
    internal val allItems: List<FridgeItem>,
    val items: List<FridgeItem>,
    val search: String,
    val isLoading: Loading?,
    val entry: FridgeEntry?,
    val sort: Sorts,
    val showing: Showing,
    val listError: Throwable?,
    val undoableItem: FridgeItem?,
    val expirationRange: ExpirationRange?,
    val isSameDayExpired: IsSameDayExpired?,
    val listItemPresence: FridgeItem.Presence,
    val counts: Counts?,
    val bottomBarHeight: Int
) : UiViewState {

    data class Counts internal constructor(
        val totalCount: Int,
        val firstCount: Int,
        val secondCount: Int,
        val thirdCount: Int
    )

    @CheckResult
    internal fun FridgeItem.matchesQuery(query: String): Boolean {
        // Empty query always matches
        return if (query.isBlank()) true else {
            this.name().contains(query, ignoreCase = true)
        }
    }

    internal val dateSorter = Comparator<FridgeItem> { o1, o2 ->
        when (sort) {
            Sorts.CREATED -> o1.createdTime().compareTo(o2.createdTime())
            Sorts.NAME -> o1.name().compareTo(o2.name(), ignoreCase = true)
            Sorts.PURCHASED -> o1.purchaseTime().compareTo(o2.purchaseTime())
            Sorts.EXPIRATION -> o1.expireTime().compareTo(o2.expireTime())
        }
    }

    // Compare dates which may be null
    // Null dates come after non-null dates
    @CheckResult
    private fun Date?.compareTo(other: Date?): Int {
        return if (this == null && other == null) 0 else {
            if (other == null) -1 else this?.compareTo(other) ?: 1
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

    data class Loading internal constructor(val isLoading: Boolean)
}

sealed class DetailViewEvent : UiViewEvent {

    object AddNewItemEvent : DetailViewEvent()

    data class SearchQuery internal constructor(val search: String) : DetailViewEvent()

    object ForceRefresh : DetailViewEvent()

    object ToggleArchiveVisibility : DetailViewEvent()

    data class ChangeSort internal constructor(val sort: DetailViewState.Sorts) : DetailViewEvent()

    data class UndoDelete internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ReallyDeleteNoUndo internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Consume internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Delete internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Restore internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Spoil internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ExpandItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ChangePresence internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class IncreaseCount internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class DecreaseCount internal constructor(val item: FridgeItem) : DetailViewEvent()
}

sealed class DetailControllerEvent : UiControllerEvent {

    data class AddNew internal constructor(val id: FridgeEntry.Id) : DetailControllerEvent()

    data class ExpandForEditing internal constructor(
        val item: FridgeItem
    ) : DetailControllerEvent()

    object EntryArchived : DetailControllerEvent()
}
