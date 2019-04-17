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

import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbar.Callback
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarViewModel.ToolbarState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

internal class ShoppingToolbarViewModel @Inject internal constructor(
  private val handler: UiEventHandler<ToolbarEvent, Callback>
) : UiViewModel<ToolbarState>(
  initialState = ToolbarState(isNavigate = false)
), Callback {

  override fun onBind() {
    handler.handle(this).destroy()
  }

  override fun onUnbind() {
  }

  override fun onNavigationClicked() {
    setUniqueState(true, old = { it.isNavigate }) { state, value ->
      state.copy(isNavigate = value)
    }
  }

  data class ToolbarState(val isNavigate: Boolean) : UiState
}

