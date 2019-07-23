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

package com.pyamsoft.fridge.detail.item

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class DetailItemViewState internal constructor(
  val item: FridgeItem,
  val isEditable: Boolean,
  val throwable: Throwable?
) : UiViewState

sealed class DetailItemViewEvent : UiViewEvent {

  data class ExpandItem internal constructor(val item: FridgeItem) : DetailItemViewEvent()

  data class CommitName internal constructor(
    val oldItem: FridgeItem,
    val name: String
  ) : DetailItemViewEvent()

  data class CommitPresence internal constructor(
    val oldItem: FridgeItem,
    val presence: Presence
  ) : DetailItemViewEvent()

  data class PickDate internal constructor(
    val oldItem: FridgeItem,
    val year: Int,
    val month: Int,
    val day: Int
  ) : DetailItemViewEvent()

  data class CloseItem internal constructor(val item: FridgeItem) : DetailItemViewEvent()

  data class DeleteItem internal constructor(val item: FridgeItem) : DetailItemViewEvent()

  data class ConsumeItem internal constructor(val item: FridgeItem) : DetailItemViewEvent()

  data class SpoilItem internal constructor(val item: FridgeItem) : DetailItemViewEvent()

}

sealed class DetailItemControllerEvent : UiControllerEvent {

  data class ExpandDetails internal constructor(val item: FridgeItem) : DetailItemControllerEvent()

  data class DatePick internal constructor(
    val oldItem: FridgeItem,
    val year: Int,
    val month: Int,
    val day: Int
  ) : DetailItemControllerEvent()

  object CloseExpand : DetailItemControllerEvent()

}
