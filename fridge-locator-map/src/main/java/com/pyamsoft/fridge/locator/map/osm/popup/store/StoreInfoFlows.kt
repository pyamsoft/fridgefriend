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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import org.osmdroid.views.overlay.Marker

data class StoreInfoViewState internal constructor(
    val marker: Marker?,
    val cached: StoreCached?
) : UiViewState {

    data class StoreCached internal constructor(val cached: Boolean)
}

sealed class StoreInfoViewEvent : UiViewEvent {

    data class StoreFavoriteAction internal constructor(
        val store: NearbyStore,
        val add: Boolean
    ) : StoreInfoViewEvent()
}

sealed class StoreInfoControllerEvent : UiControllerEvent
