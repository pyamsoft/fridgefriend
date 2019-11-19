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

package com.pyamsoft.fridge.locator.map.osm

import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class OsmViewState internal constructor(
    val loading: Boolean,
    val points: List<NearbyStore>,
    val zones: List<NearbyZone>,
    val nearbyError: Throwable?,
    val cachedFetchError: Throwable?,

    // Hacky view-to-view interaction via one shot setState calls in the VM
    val requestMapCenter: MapCenterRequest?
) : UiViewState {

    data class MapCenterRequest internal constructor(val automatic: Boolean)
}

sealed class OsmViewEvent : UiViewEvent {

    data class UpdateBoundingBox internal constructor(internal val box: BBox) : OsmViewEvent()

    object RequestBackgroundPermission : OsmViewEvent()

    data class RequestMyLocation internal constructor(val automatic: Boolean) : OsmViewEvent()

    object RequestFindNearby : OsmViewEvent()
}

sealed class OsmControllerEvent : UiControllerEvent {

    object BackgroundPermissionRequest : OsmControllerEvent()
}
