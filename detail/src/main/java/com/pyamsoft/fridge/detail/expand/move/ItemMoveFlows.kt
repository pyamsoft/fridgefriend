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

package com.pyamsoft.fridge.detail.expand.move

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryViewState
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class ItemMoveViewState internal constructor(
    val item: FridgeItem?,
    val listState: EntryViewState,
) : UiViewState

sealed class ItemMoveViewEvent : UiViewEvent {

    data class SearchQuery(val search: String) : ItemMoveViewEvent()

    data class ChangeSort(val sort: EntryViewState.Sorts) : ItemMoveViewEvent()

    object Close : ItemMoveViewEvent()
}

sealed class ItemMoveListControllerEvent : UiControllerEvent {

    data class SelectedTarget internal constructor(
        val entry: FridgeEntry,
    ) : ItemMoveListControllerEvent()

}

sealed class ItemMoveControllerEvent : UiControllerEvent {

    object Close : ItemMoveControllerEvent()

    data class PublishSearch(val search: String) : ItemMoveControllerEvent()

    data class PublishSort(val sort: EntryViewState.Sorts) : ItemMoveControllerEvent()
}
