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
import com.pyamsoft.fridge.setting.databinding.SettingFrameBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import javax.inject.Inject

class SettingFrame @Inject internal constructor(
    parent: ViewGroup
) : BindingUiView<SettingsViewState, SettingsViewEvent, SettingFrameBinding>(parent) {

    override val viewBinding by viewBinding(SettingFrameBinding::inflate)

    override val layoutRoot by boundView { settingFrame }

    override fun onRender(state: SettingsViewState) {
    }
}
