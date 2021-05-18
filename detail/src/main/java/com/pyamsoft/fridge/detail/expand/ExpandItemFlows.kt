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

package com.pyamsoft.fridge.detail.expand

import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class ExpandedViewState
internal constructor(
    val item: FridgeItem?,
    val throwable: Throwable?,
    val sameNamedItems: Collection<FridgeItem>,
    val similarItems: Collection<SimilarItem>,
    val categories: List<FridgeCategory>,
) : UiViewState {

  data class SimilarItem internal constructor(val item: FridgeItem?, val display: String)
}

sealed class ExpandedViewEvent : UiViewEvent {

  sealed class ItemEvent : ExpandedViewEvent() {

    data class CommitCategory internal constructor(val index: Int) : ItemEvent()

    data class CommitName internal constructor(val name: String) : ItemEvent()

    data class CommitCount internal constructor(val count: Int) : ItemEvent()

    data class SelectSimilar internal constructor(val item: FridgeItem) : ItemEvent()

    object CommitPresence : ItemEvent()

    object PickDate : ItemEvent()
  }

  sealed class ToolbarEvent : ExpandedViewEvent() {

    object CloseItem : ToolbarEvent()

    object DeleteItem : ToolbarEvent()

    object ConsumeItem : ToolbarEvent()

    object SpoilItem : ToolbarEvent()

    object RestoreItem : ToolbarEvent()

    object MoveItem : ToolbarEvent()
  }
}

sealed class ExpandedControllerEvent : UiControllerEvent {

  object Close : ExpandedControllerEvent()

  data class MoveItem internal constructor(val item: FridgeItem) : ExpandedControllerEvent()

  data class DatePicked
  internal constructor(
      val oldItem: FridgeItem,
      val year: Int,
      val month: Int,
      val day: Int,
  ) : ExpandedControllerEvent()
}
