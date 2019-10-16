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

import android.location.Location
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoViewEvent.StoreFavoriteAction
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoViewState.StoreCached
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Marker
import javax.inject.Inject

internal class StoreInfoViewModel @Inject internal constructor(
    private val interactor: StoreInfoInteractor,
    store: NearbyStore
) : UiViewModel<StoreInfoViewState, StoreInfoViewEvent, StoreInfoControllerEvent>(
    initialState = StoreInfoViewState(
        myLocation = null,
        marker = null,
        cached = null
    )
) {

    private val storeId = store.id()

    init {
        doOnInit {
            findCachedStoreIfExists()
            listenForRealtime()
        }
    }

    private fun listenForRealtime() {
        viewModelScope.launch {
            interactor.listenForNearbyCacheChanges(storeId,
                onInsert = {
                    setState { copy(cached = StoreCached(true)) }
                },
                onDelete = {
                    setState { copy(cached = StoreCached(false)) }
                })
        }
    }

    private fun findCachedStoreIfExists() {
        viewModelScope.launch {
            val isCached = interactor.isNearbyStoreCached(storeId)
            setState { copy(cached = StoreCached(isCached)) }
        }
    }

    override fun handleViewEvent(event: StoreInfoViewEvent) {
        return when (event) {
            is StoreFavoriteAction -> handleStoreFavoriteAction(event.store, event.add)
        }
    }

    private fun handleStoreFavoriteAction(
        zone: NearbyStore,
        add: Boolean
    ) {
        viewModelScope.launch {
            if (add) {
                interactor.insertStoreIntoDb(zone)
            } else {
                interactor.deleteStoreFromDb(zone)
            }
        }
    }

    fun updateMarker(marker: Marker) {
        setState { copy(marker = marker) }
    }

    fun handleLocationUpdate(location: Location?) {
        setState { copy(myLocation = location) }
    }
}
