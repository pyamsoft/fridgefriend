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

import android.location.Location
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import org.osmdroid.views.overlay.Polygon

data class ZoneInfoViewState internal constructor(
    val myLocation: Location?,
    val polygon: Polygon?,
    val cached: ZoneCached?
) : UiViewState {

    data class ZoneCached internal constructor(val cached: Boolean)
}

sealed class ZoneInfoViewEvent : UiViewEvent {

    data class ZoneFavoriteAction internal constructor(
        val zone: NearbyZone,
        val add: Boolean
    ) : ZoneInfoViewEvent()
}

sealed class ZoneInfoControllerEvent : UiControllerEvent
