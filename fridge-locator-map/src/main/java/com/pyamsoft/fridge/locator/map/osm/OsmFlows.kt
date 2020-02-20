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
    val boundingBox: BBox?,
    val loading: Boolean,
    val points: List<NearbyStore>,
    val zones: List<NearbyZone>,
    val nearbyError: Throwable?,
    val cachedFetchError: Throwable?,
    val centerMyLocation: CenterMyLocation?
) : UiViewState {

    data class CenterMyLocation internal constructor(val firstTime: Boolean)
}

sealed class OsmViewEvent : UiViewEvent {

    data class UpdateBoundingBox internal constructor(internal val box: BBox) : OsmViewEvent()

    data class RequestMyLocation internal constructor(val firstTime: Boolean) : OsmViewEvent()

    object DoneFindingMyLocation : OsmViewEvent()

    object RequestFindNearby : OsmViewEvent()
}

sealed class OsmControllerEvent : UiControllerEvent
