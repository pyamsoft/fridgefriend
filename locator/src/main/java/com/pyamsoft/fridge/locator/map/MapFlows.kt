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

package com.pyamsoft.fridge.locator.map


import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class MapViewState internal constructor(
    val boundingBox: BBox?,
    val loading: Boolean,
    val points: List<MapPoint>,
    val zones: List<MapZone>,
    val nearbyError: Throwable?,
    val gpsError: Throwable?,
    val cachedFetchError: Throwable?,
    val centerMyLocation: CenterMyLocation?,
    val bottomOffset: Int,
) : UiViewState {

    data class MapPoint internal constructor(
        val point: NearbyStore,
        val forceOpen: Boolean,
    )

    data class MapZone internal constructor(
        val zone: NearbyZone,
        val forceOpen: Boolean,
    )

    data class CenterMyLocation internal constructor(val firstTime: Boolean)
}

sealed class MapViewEvent : UiViewEvent {

    data class UpdateBoundingBox(val box: BBox) : MapViewEvent()

    data class RequestMyLocation(val firstTime: Boolean) : MapViewEvent()

    object DoneFindingMyLocation : MapViewEvent()

    object RequestFindNearby : MapViewEvent()

    object HideFetchError : MapViewEvent()

    object HideCacheError : MapViewEvent()

    data class OpenPopup(val popup: MapPopup) : MapViewEvent()
}

sealed class MapControllerEvent : UiControllerEvent {

    data class PopupClicked(val popup: MapPopupOverlay) : MapControllerEvent()
}
