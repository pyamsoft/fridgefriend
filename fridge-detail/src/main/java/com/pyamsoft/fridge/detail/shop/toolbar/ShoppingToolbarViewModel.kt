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

package com.pyamsoft.fridge.detail.shop.toolbar

import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarViewEvent.Close
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitViewState
import javax.inject.Inject

class ShoppingToolbarViewModel @Inject internal constructor(
) : UiViewModel<UnitViewState, ShoppingToolbarViewEvent, ShoppingToolbarControllerEvent>(
    initialState = UnitViewState
) {

  override fun handleViewEvent(event: ShoppingToolbarViewEvent) {
    return when (event) {
      is Close -> publish(NavigateUp)
    }
  }
}

