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

package com.pyamsoft.fridge.locator.permission

import android.view.ViewGroup
import com.pyamsoft.fridge.locator.databinding.PermissionExplanationBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.arch.UnitViewState
import javax.inject.Inject

class LocationExplanation @Inject internal constructor(
    parent: ViewGroup
) : BindingUiView<UnitViewState, PermissionViewEvent, PermissionExplanationBinding>(parent) {

    override val viewBinding = PermissionExplanationBinding::inflate

    override val layoutRoot by boundView { locationPermissionExplanation }

    override fun onRender(state: UnitViewState) {
    }
}
