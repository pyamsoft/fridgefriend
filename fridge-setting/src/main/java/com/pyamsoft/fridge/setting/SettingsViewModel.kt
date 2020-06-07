/*
 * Copyright 2020 Peter Kenji Yamanaka
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
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import com.pyamsoft.pydroid.arch.UnitViewEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

class SettingsViewModel @Inject internal constructor(
    bottomOffsetBus: EventConsumer<BottomOffset>,
    @Named("debug") debug: Boolean
) : UiViewModel<SettingsViewState, UnitViewEvent, UnitControllerEvent>(
    initialState = SettingsViewState(
        bottomOffset = 0
    ), debug = debug
) {

    init {
        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
            }
        }
    }

    override fun handleViewEvent(event: UnitViewEvent) {
    }
}
