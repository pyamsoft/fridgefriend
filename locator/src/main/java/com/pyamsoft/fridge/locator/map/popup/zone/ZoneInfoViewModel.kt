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

package com.pyamsoft.fridge.locator.map.popup.zone

import android.location.Location
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoViewModel
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoViewState
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoViewEvent.ZoneFavoriteAction
import javax.inject.Inject

class ZoneInfoViewModel @Inject internal constructor(
    interactor: ZoneInfoInteractor,
    zone: NearbyZone,
) : BaseInfoViewModel<NearbyZone, ZoneInfoViewState, ZoneInfoViewEvent, ZoneInfoControllerEvent>(
    interactor,
    initialState = ZoneInfoViewState(
        myLocation = null,
        cached = null,
        name = "",
        latitude = null,
        longitude = null
    )
) {

    private val zoneId = zone.id()

    override fun isDataMatch(data: NearbyZone): Boolean {
        return data.id() == zoneId
    }

    override fun handleViewEvent(event: ZoneInfoViewEvent) = when (event) {
        is ZoneFavoriteAction -> handleFavoriteAction(event.zone, event.add)
    }

    override fun copyState(
        state: ZoneInfoViewState,
        myLocation: Location?,
        cached: BaseInfoViewState.Cached?,
        name: String,
        latitude: Double?,
        longitude: Double?
    ): ZoneInfoViewState {
        return state.copy(
            myLocation = myLocation,
            cached = cached,
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }

}
