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

package com.pyamsoft.fridge.detail.expand

import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class ExpandItemViewState internal constructor(
    val item: FridgeItem?,
    val throwable: Throwable?,
    val sameNamedItems: Collection<FridgeItem>,
    val similarItems: Collection<SimilarItem>,
    val categories: List<FridgeCategory>,
) : UiViewState {

    data class SimilarItem internal constructor(val item: FridgeItem?, val display: String)
}

sealed class ExpandedItemViewEvent : UiViewEvent {

    data class CommitCategory internal constructor(val index: Int) : ExpandedItemViewEvent()

    data class CommitName internal constructor(val name: String) : ExpandedItemViewEvent()

    data class CommitCount internal constructor(val count: Int) : ExpandedItemViewEvent()

    data class SelectSimilar internal constructor(val item: FridgeItem) : ExpandedItemViewEvent()

    object CommitPresence : ExpandedItemViewEvent()

    object PickDate : ExpandedItemViewEvent()

    object CloseItem : ExpandedItemViewEvent()

    object DeleteItem : ExpandedItemViewEvent()

    object ConsumeItem : ExpandedItemViewEvent()

    object SpoilItem : ExpandedItemViewEvent()

    object RestoreItem : ExpandedItemViewEvent()

    object MoveItem : ExpandedItemViewEvent()
}

sealed class ExpandItemControllerEvent : UiControllerEvent {

    data class DatePick internal constructor(
        val oldItem: FridgeItem,
        val year: Int,
        val month: Int,
        val day: Int,
    ) : ExpandItemControllerEvent()

    object CloseExpand : ExpandItemControllerEvent()

    data class BeginMove internal constructor(
        val item: FridgeItem,
    ) : ExpandItemControllerEvent()
}
