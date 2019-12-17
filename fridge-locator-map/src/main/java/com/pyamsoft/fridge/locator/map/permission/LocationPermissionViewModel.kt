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

package com.pyamsoft.fridge.locator.map.permission

import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.map.permission.PermissionControllerEvent.LocationPermissionRequest
import com.pyamsoft.fridge.locator.map.permission.PermissionViewEvent.FireLocationPermission
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UnitViewState
import javax.inject.Inject

class LocationPermissionViewModel @Inject internal constructor(
    private val mapPermission: MapPermission
) : UiViewModel<UnitViewState, PermissionViewEvent, PermissionControllerEvent>(
    initialState = UnitViewState
) {

    override fun handleViewEvent(event: PermissionViewEvent) {
        return when (event) {
            is FireLocationPermission -> publish(LocationPermissionRequest)
        }
    }

    fun requestForegroundPermission(consumer: PermissionConsumer<ForegroundLocationPermission>) {
        mapPermission.requestForegroundPermission(consumer)
    }
}
