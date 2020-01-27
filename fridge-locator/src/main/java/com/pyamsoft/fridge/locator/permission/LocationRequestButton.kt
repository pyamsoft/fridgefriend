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
import android.widget.Button
import com.pyamsoft.fridge.locator.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class LocationRequestButton @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<UnitViewState, PermissionViewEvent>(parent) {

    override val layout: Int = R.layout.permission_button

    override val layoutRoot by boundView<Button>(R.id.location_permission_button)

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener { publish(PermissionViewEvent.FireLocationPermission) }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }
    }

    override fun onRender(state: UnitViewState) {
    }
}
