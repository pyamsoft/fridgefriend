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

package com.pyamsoft.fridge.locator.map.popup.store

import android.location.Location
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoViewModel
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoViewState
import com.pyamsoft.fridge.locator.map.popup.store.StoreInfoViewEvent.StoreFavoriteAction
import javax.inject.Inject

class StoreInfoViewModel @Inject internal constructor(
    interactor: StoreInfoInteractor,
    store: NearbyStore,
) : BaseInfoViewModel<NearbyStore, StoreInfoViewState, StoreInfoViewEvent, StoreInfoControllerEvent>(
    interactor,
    initialState = StoreInfoViewState(
        myLocation = null,
        cached = null,
        name = "",
        latitude = null,
        longitude = null
    )
) {

    private val storeId = store.id()

    override fun isDataMatch(data: NearbyStore): Boolean {
        return data.id() == storeId
    }

    override fun handleViewEvent(event: StoreInfoViewEvent) = when (event) {
        is StoreFavoriteAction -> handleFavoriteAction(event.store, event.add)
    }

    override fun copyState(
        state: StoreInfoViewState,
        myLocation: Location?,
        cached: BaseInfoViewState.Cached?,
        name: String,
        latitude: Double?,
        longitude: Double?
    ): StoreInfoViewState {
        return state.copy(
            myLocation = myLocation,
            cached = cached,
            name = name,
            latitude = latitude,
            longitude = longitude
        )
    }

}
