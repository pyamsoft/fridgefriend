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

import android.view.ViewGroup
import com.pyamsoft.fridge.setting.SettingsViewEvent.Navigate
import com.pyamsoft.fridge.setting.databinding.SettingToolbarBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class SettingToolbar @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<SettingsViewState, SettingsViewEvent, SettingToolbarBinding>(parent) {

    override val viewBinding = SettingToolbarBinding::inflate

    override val layoutRoot by boundView { settingToolbar }

    init {
        doOnInflate {
            binding.settingToolbar.setUpEnabled(true)
            binding.settingToolbar.setNavigationOnClickListener(DebouncedOnClickListener.create {
                publish(Navigate)
            })
        }

        doOnTeardown {
            binding.settingToolbar.setUpEnabled(false)
            binding.settingToolbar.setNavigationOnClickListener(null)
        }
    }

    override fun onRender(state: SettingsViewState) {
        binding.settingToolbar.title = state.name
    }
}
