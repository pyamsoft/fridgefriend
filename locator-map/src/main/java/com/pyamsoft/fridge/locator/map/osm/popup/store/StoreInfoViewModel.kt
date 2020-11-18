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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import android.location.Location
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoViewModel
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoViewState
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoViewEvent.StoreFavoriteAction
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

internal class StoreInfoViewModel @Inject internal constructor(
    private val interactor: StoreInfoInteractor,
    store: NearbyStore,
) : BaseInfoViewModel<NearbyStore, StoreInfoViewState, StoreInfoViewEvent, StoreInfoControllerEvent>(
    interactor,
    initialState = StoreInfoViewState(
        myLocation = null,
        data = null,
        cached = null
    )
) {

    private val storeId = store.id()

    override suspend fun listenForRealtime() {
        interactor.listenForNearbyCacheChanges(
            onInsert = { store ->
                if (store.id() == storeId) {
                    setState { copy(cached = BaseInfoViewState.Cached(true)) }
                }
            },
            onDelete = { store ->
                if (store.id() == storeId) {
                    setState { copy(cached = BaseInfoViewState.Cached(false)) }
                }
            })
    }

    override suspend fun restoreStateFromCachedData(cached: List<NearbyStore>) {
        setState { copy(cached = BaseInfoViewState.Cached(cached = cached.any { it.id() == storeId })) }
    }

    override fun handleViewEvent(event: StoreInfoViewEvent) {
        return when (event) {
            is StoreFavoriteAction -> handleFavoriteAction(event.store, event.add)
        }
    }

    override fun handleLocationUpdate(location: Location?) {
        setState {
            val newLocation = processLocationUpdate(location)
            return@setState if (newLocation == null) this else copy(myLocation = newLocation.location)
        }
    }

    fun updateMarker(marker: Marker) {
        setState { copy(data = marker) }
    }
}
