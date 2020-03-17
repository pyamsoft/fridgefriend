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
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import java.util.Date

data class DetailViewState(
    internal val items: List<FridgeItem>,
    val isLoading: Loading?,
    val entry: FridgeEntry?,
    val sort: Sorts,
    val showing: Showing,
    val listError: Throwable?,
    val undoableItem: FridgeItem?,
    val expirationRange: ExpirationRange?,
    val isSameDayExpired: IsSameDayExpired?,
    val listItemPresence: FridgeItem.Presence,
    val isItemExpanded: Boolean
) : UiViewState {

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
    fun getShowingItems(): List<FridgeItem> {
        return items.filter {
            when (showing) {
                Showing.FRESH -> !it.isArchived()
                Showing.CONSUMED -> it.isConsumed()
                Showing.SPOILED -> it.isSpoiled()
            }
        }
    }

    @CheckResult
    fun getTotalItemCount(): Int {
        return getFreshItemCount() + getSpoiledItemCount() + getConsumedItemCount()
    }

    @CheckResult
    fun getFreshItemCount(): Int {
        return filterValid { items.filterNot { it.isArchived() } }.size
    }

    @CheckResult
    fun getSpoiledItemCount(): Int {
        return filterValid { items.filter { it.isSpoiled() } }.size
    }

    @CheckResult
    fun getConsumedItemCount(): Int {
        return filterValid { items.filter { it.isConsumed() } }.size
    }

    @CheckResult
    private inline fun filterValid(func: DetailViewState.() -> List<FridgeItem>): List<FridgeItem> {
        return this.func().filterNot { it.isEmpty() }
    }

    data class ExpirationRange internal constructor(val range: Int)

    data class IsSameDayExpired internal constructor(val isSame: Boolean)

    data class Loading internal constructor(val isLoading: Boolean)
}

sealed class DetailViewEvent : UiViewEvent {

    object AddNewItemEvent : DetailViewEvent()

    object ForceRefresh : DetailViewEvent()

    object ToggleArchiveVisibility : DetailViewEvent()

    data class UndoDelete internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ReallyDeleteNoUndo internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Consume internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Delete internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Restore internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class Spoil internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ExpandItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ChangePresence internal constructor(val item: FridgeItem) : DetailViewEvent()
}

sealed class DetailControllerEvent : UiControllerEvent {

    data class AddNew internal constructor(val id: FridgeEntry.Id) : DetailControllerEvent()

    data class ExpandForEditing internal constructor(
        val item: FridgeItem
    ) : DetailControllerEvent()

    object EntryArchived : DetailControllerEvent()
}
