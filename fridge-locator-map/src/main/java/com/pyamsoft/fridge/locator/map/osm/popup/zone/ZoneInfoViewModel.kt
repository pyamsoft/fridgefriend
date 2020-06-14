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
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoViewModel
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoViewState
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoViewEvent.ZoneFavoriteAction
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Polygon

internal class ZoneInfoViewModel @Inject internal constructor(
    private val interactor: ZoneInfoInteractor,
    zone: NearbyZone,
    @Named("debug") debug: Boolean
) : BaseInfoViewModel<NearbyZone, ZoneInfoViewState, ZoneInfoViewEvent, ZoneInfoControllerEvent>(
    interactor,
    initialState = ZoneInfoViewState(
        myLocation = null,
        data = null,
        cached = null
    ),
    debug = debug
) {

    private val zoneId = zone.id()

    override fun listenForRealtime() {
        viewModelScope.launch(context = Dispatchers.Default) {
            interactor.listenForNearbyCacheChanges(
                onInsert = { zone ->
                    if (zone.id() == zoneId) {
                        setState { copy(cached = BaseInfoViewState.Cached(true)) }
                    }
                },
                onDelete = { zone ->
                    if (zone.id() == zoneId) {
                        setState { copy(cached = BaseInfoViewState.Cached(false)) }
                    }
                })
        }
    }

    override fun restoreStateFromCachedData(cached: List<NearbyZone>) {
        setState { copy(cached = BaseInfoViewState.Cached(cached = cached.any { it.id() == zoneId })) }
    }

    override fun handleViewEvent(event: ZoneInfoViewEvent) {
        return when (event) {
            is ZoneFavoriteAction -> handleZoneFavoriteAction(event.zone, event.add)
        }
    }

    private fun handleZoneFavoriteAction(
        zone: NearbyZone,
        add: Boolean
    ) {
        viewModelScope.launch(context = Dispatchers.Default) {
            if (add) {
                interactor.insertIntoDb(zone)
            } else {
                interactor.deleteFromDb(zone)
            }
        }
    }

    override fun handleLocationUpdate(location: Location?) {
        setState {
            val newLocation = processLocationUpdate(location)
            return@setState if (newLocation == null) this else copy(myLocation = newLocation.location)
        }
    }

    fun updatePolygon(polygon: Polygon) {
        setState { copy(data = polygon) }
    }
}
