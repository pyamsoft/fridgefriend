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

package com.pyamsoft.fridge.entry.action

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class EntryActionViewState internal constructor(
  val spacing: Spacing,
  val throwable: Throwable?
) : UiViewState {

  data class Spacing(
    val isLaidOut: Boolean,
    val isFirstAnimationDone: Boolean,
    val gap: Int,
    val margin: Int
  )
}

sealed class EntryActionViewEvent : UiViewEvent {

  data class SpacingCalculated internal constructor(
    val gap: Int,
    val margin: Int
  ) : EntryActionViewEvent()

  object FirstAnimationDone : EntryActionViewEvent()

  object CreateClicked : EntryActionViewEvent()

  object ShopClicked : EntryActionViewEvent()

}

sealed class EntryActionControllerEvent : UiControllerEvent {

  data class OpenCreate(val entry: FridgeEntry) : EntryActionControllerEvent()

  object OpenShopping : EntryActionControllerEvent()

}
