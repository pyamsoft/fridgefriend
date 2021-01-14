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
 */

package com.pyamsoft.fridge.locator.permission

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitViewState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class LocationPermissionViewModel @Inject internal constructor(
    private val mapPermission: MapPermission,
) : UiViewModel<UnitViewState, PermissionViewEvent, PermissionControllerEvent>(UnitViewState) {

    override fun handleViewEvent(event: PermissionViewEvent) = when (event) {
        is PermissionViewEvent.FireLocationPermission -> publish(PermissionControllerEvent.LocationPermissionRequest)
    }

    fun requestForegroundPermission(consumer: PermissionConsumer<ForegroundLocationPermission>) {
        viewModelScope.launch(context = Dispatchers.Default) {
            mapPermission.requestForegroundPermission(consumer)
        }
    }
}
