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

package com.pyamsoft.fridge.locator.map.osm.popup.zone

import android.view.ViewGroup
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoLocation
import javax.inject.Inject

internal class ZoneInfoLocation @Inject internal constructor(
    parent: ViewGroup
) : BaseInfoLocation<ZoneInfoViewState, ZoneInfoViewEvent>(parent) {

    private fun handleLocation(state: ZoneInfoViewState) {
        val polygon = state.data
        val centerPoint = polygon?.infoWindowLocation
        displayLocation(
            centerPoint?.latitude,
            centerPoint?.longitude,
            state.myLocation,
            centerPoint
        )
    }

    override fun onRender(state: ZoneInfoViewState) {
        layoutRoot.post { handleLocation(state) }
    }
}
