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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.setting.SettingsControllerEvent.NavigateUp
import com.pyamsoft.fridge.setting.SettingsViewEvent.Navigate
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject internal constructor(
    @Named("debug") debug: Boolean,
    titleBus: EventBus<SettingsTitleChange>
) : UiViewModel<SettingsViewState, SettingsViewEvent, SettingsControllerEvent>(
    initialState = SettingsViewState(name = "Settings"), debug = debug
) {

    init {
        doOnInit {
            viewModelScope.launch {
                titleBus.scopedEvent { setState { copy(name = it.title) } }
            }
        }

        doOnInit { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(KEY_TITLE) { name ->
                setState { copy(name = name) }
            }
        }

        doOnSaveState { state ->
            put(KEY_TITLE, state.name)
        }
    }

    override fun handleViewEvent(event: SettingsViewEvent) {
        return when (event) {
            is Navigate -> publish(NavigateUp)
        }
    }

    companion object {

        private const val KEY_TITLE = "key_title"
    }
}
