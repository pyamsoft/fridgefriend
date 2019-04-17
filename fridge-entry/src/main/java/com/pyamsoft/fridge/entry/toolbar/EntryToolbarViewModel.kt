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

package com.pyamsoft.fridge.entry.toolbar

import com.pyamsoft.fridge.entry.toolbar.EntryToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarViewModel.ToolbarState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

internal class EntryToolbarViewModel @Inject internal constructor(
  private val handler: UiEventHandler<ToolbarEvent, EntryToolbar.Callback>
) : UiViewModel<ToolbarState>(
  initialState = ToolbarState(isSettings = false)
), EntryToolbar.Callback {

  override fun onBind() {
    handler.handle(this).destroy()
  }

  override fun onUnbind() {
  }

  override fun onSettingsClicked() {
    setUniqueState(true, old = { it.isSettings }) { state, value ->
      state.copy(isSettings = value)
    }
  }

  data class ToolbarState(val isSettings: Boolean) : UiState
}

