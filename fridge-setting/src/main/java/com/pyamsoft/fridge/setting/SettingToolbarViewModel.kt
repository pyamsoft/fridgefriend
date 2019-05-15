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

package com.pyamsoft.fridge.setting

import com.pyamsoft.fridge.setting.SettingToolbarControllerEvent.NavigateUp
import com.pyamsoft.fridge.setting.SettingToolbarViewEvent.ToolbarNavigate
import com.pyamsoft.pydroid.arch.impl.BaseUiViewModel
import com.pyamsoft.pydroid.arch.impl.UnitViewState
import javax.inject.Inject

class SettingToolbarViewModel @Inject internal constructor(
) : BaseUiViewModel<UnitViewState, SettingToolbarViewEvent, SettingToolbarControllerEvent>(
    initialState = UnitViewState
) {

  override fun handleViewEvent(event: SettingToolbarViewEvent) {
    return when (event) {
      is ToolbarNavigate -> publish(NavigateUp)
    }
  }
}
