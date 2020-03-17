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

data class DetailViewState(
    val items: List<FridgeItem>,
    val isLoading: Loading?,
    val entry: FridgeEntry?,
    val showing: Showing,
    val listError: Throwable?,
    val undoableItem: FridgeItem?,
    val expirationRange: ExpirationRange?,
    val isSameDayExpired: IsSameDayExpired?,
    val listItemPresence: FridgeItem.Presence,
    val isItemExpanded: Boolean
) : UiViewState {

    enum class Showing {
        FRESH,
        CONSUMED,
        SPOILED
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
