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

import android.app.Activity
import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class MapViewModel @Inject internal constructor(
    private val mapPermission: MapPermission,
    private val interactor: MapInteractor,
    private val deviceGps: DeviceGps,
    bottomOffsetBus: EventConsumer<BottomOffset>,
) : UiViewModel<MapViewState, MapViewEvent, MapControllerEvent>(
    MapViewState(
        boundingBox = null,
        loading = false,
        points = emptyList(),
        zones = emptyList(),
        centerMyLocation = null,
        nearbyError = null,
        cachedFetchError = null,
        gpsError = null,
        bottomOffset = 0
    )
) {

    private val nearbyRunner = highlander<
            Unit,
            BBox?,
            NearbyStore.Id,
            NearbyZone.Id
            > { box, storeId, zoneId ->
        try {
            setState { copy(loading = true) }

            // Run jobs in parallel
            val jobs = mutableListOf<Deferred<*>>()
            jobs += async(context = Dispatchers.Default) {
                try {
                    updateMarkers(interactor.fromCache(), storeId, zoneId)
                } catch (error: Throwable) {
                    error.onActualError { e ->
                        Timber.e(e, "Error getting cached supermarkets")
                        handleCachedFetchError(e)
                    }
                }
            }

            if (box != null) {
                jobs += async(context = Dispatchers.Default) {
                    try {
                        updateMarkers(interactor.nearbyLocations(box), storeId, zoneId)
                    } catch (error: Throwable) {
                        error.onActualError { e ->
                            Timber.e(e, "Error fetching nearby supermarkets")
                            handleNearbyError(e)
                        }
                    }
                }
            }

            jobs.awaitAll()
        } finally {
            setState { copy(loading = false) }
        }
    }

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height).resetForceOpen() } }
        }
    }

    private fun handleNearbyError(throwable: Throwable?) {
        setState { copy(nearbyError = throwable).resetForceOpen() }
    }

    private fun updateMarkers(
        markers: MapMarkers,
        storeId: NearbyStore.Id,
        zoneId: NearbyZone.Id,
    ) {
        setState {
            val newPoints = markers.points.map {
                MapViewState.MapPoint(point = it, forceOpen = it.id() == storeId)
            }
            val newZones = markers.zones.map {
                MapViewState.MapZone(zone = it, forceOpen = it.id() == zoneId)
            }
            copy(
                points = merge(points, newPoints) { it.point.id() },
                zones = merge(zones, newZones) { it.zone.id() },
                cachedFetchError = null,
                nearbyError = null
            )
        }
    }

    private inline fun <T : Any, ID : Any> merge(
        oldList: List<T>,
        newList: List<T>,
        id: (item: T) -> ID,
    ): List<T> {
        val result = ArrayList(newList)
        oldList.forEach { oldItem ->
            val oldId = id(oldItem)
            // If the new list doesn't have the old id, add the old id
            // Otherwise, the new item is newer.
            if (result.find { id(it) == oldId } == null) {
                result.add(oldItem)
            }
        }
        return result
    }

    private fun handleCachedFetchError(throwable: Throwable?) {
        setState { copy(cachedFetchError = throwable).resetForceOpen() }
    }

    override fun handleViewEvent(event: MapViewEvent) {
        return when (event) {
            is MapViewEvent.MapEvent.UpdateBoundingBox -> handleBoundingBox(event.box)
            is MapViewEvent.MapEvent.OpenPopup -> openPopup(event.popup)
            is MapViewEvent.MapEvent.DoneFindingMyLocation -> doneFindingMyLocation()
            is MapViewEvent.MapEvent.FindMyLocation -> findMyLocation(true)
            is MapViewEvent.ActionEvent.RequestMyLocation -> findMyLocation(false)
            is MapViewEvent.ActionEvent.RequestFindNearby -> nearbySupermarkets()
            is MapViewEvent.ActionEvent.HideFetchError -> handleNearbyError(null)
            is MapViewEvent.ActionEvent.HideCacheError -> handleCachedFetchError(null)
        }
    }

    private fun handleBoundingBox(box: BBox) {
        setState { copy(boundingBox = box).resetForceOpen() }
    }

    @CheckResult
    private fun MapViewState.resetForceOpen(): MapViewState {
        return this.copy(
            points = this.points.map { MapViewState.MapPoint(it.point, forceOpen = false) },
            zones = this.zones.map { MapViewState.MapZone(it.zone, forceOpen = false) }
        )
    }

    private fun openPopup(popup: MapPopup) {
        publish(MapControllerEvent.PopupClicked(popup))
    }

    private fun findMyLocation(firstTime: Boolean) {
        setState {
            copy(centerMyLocation = MapViewState.CenterMyLocation(firstTime)).resetForceOpen()
        }
    }

    private fun doneFindingMyLocation() {
        setState { copy(centerMyLocation = null).resetForceOpen() }
    }

    private fun nearbySupermarkets() {
        state.boundingBox?.let { fetchNearby(it, NearbyStore.Id.EMPTY, NearbyZone.Id.EMPTY) }
    }

    private fun fetchNearby(box: BBox?, storeId: NearbyStore.Id, zoneId: NearbyZone.Id) {
        viewModelScope.launch(context = Dispatchers.Default) {
            nearbyRunner.call(box, storeId, zoneId)
        }
    }

    private suspend fun resolveError(resolution: DeviceGps.ResolvableError, activity: Activity) {
        withContext(context = Dispatchers.Main) {
            try {
                Timber.w("Resolvable error when enabling GPS, try resolve")
                resolution.resolve(activity)
            } catch (e: Throwable) {
                Timber.e(e, "Error during resolution of enable GPS error")
                setState { copy(gpsError = e).resetForceOpen() }
            }
        }
    }

    fun enableGps(activity: Activity) {
        viewModelScope.launch(context = Dispatchers.Default) {
            if (!mapPermission.hasForegroundPermission()) {
                Timber.w("Missing required foreground permission!")
                return@launch
            }

            if (!deviceGps.isGpsEnabled()) {
                Timber.d("Attempt enable GPS")
                try {
                    deviceGps.enableGps()
                } catch (e: Throwable) {
                    if (e is DeviceGps.ResolvableError) {
                        Timber.d(e, "Resolve GPS enable error")
                        resolveError(e, activity)
                    } else {
                        Timber.e(e, "Error during enable GPS")
                        setState { copy(gpsError = e).resetForceOpen() }
                    }
                }
            }
        }
    }

    fun fetchNearby(storeId: NearbyStore.Id, zoneId: NearbyZone.Id) {
        fetchNearby(null, storeId, zoneId)
    }

}
