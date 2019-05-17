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

import com.pyamsoft.fridge.entry.toolbar.EntryToolbarControllerEvent.NavigateToSettings
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarViewEvent.SettingsNavigate
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

class EntryToolbarViewModel @Inject internal constructor(
) : UiViewModel<EntryToolbarViewState, EntryToolbarViewEvent, EntryToolbarControllerEvent>(
    initialState = EntryToolbarViewState(isMenuVisible = true)
) {

  override fun handleViewEvent(event: EntryToolbarViewEvent) {
    return when (event) {
      is SettingsNavigate -> publish(NavigateToSettings)
    }
  }

  fun showMenu(visible: Boolean) {
    setState { copy(isMenuVisible = visible) }
  }
}

