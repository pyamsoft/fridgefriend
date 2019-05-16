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

package com.pyamsoft.fridge.detail.list

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class DetailListViewState(
  val isLoading: Loading?,
  val throwable: Throwable?,
  val items: List<FridgeItem>
) : UiViewState {

  data class Loading(val isLoading: Boolean)

}

sealed class DetailListViewEvent : UiViewEvent {

  object ForceRefresh : DetailListViewEvent()

  data class ExpandItem internal constructor(val item: FridgeItem) : DetailListViewEvent()

  data class PickDate internal constructor(
    val oldItem: FridgeItem,
    val year: Int,
    val month: Int,
    val day: Int
  ) : DetailListViewEvent()

}

sealed class DetailListControllerEvent : UiControllerEvent {

  data class ExpandForEditing internal constructor(
    val item: FridgeItem
  ) : DetailListControllerEvent()

  data class DatePick internal constructor(
    val oldItem: FridgeItem,
    val year: Int,
    val month: Int,
    val day: Int
  ) : DetailListControllerEvent()

}
