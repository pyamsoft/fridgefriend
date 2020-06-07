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

import android.app.Activity
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.locator.DeviceGps
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class OsmViewModel @Inject internal constructor(
    private val interactor: OsmInteractor,
    private val deviceGps: DeviceGps,
    @Named("debug") debug: Boolean,
    bottomOffsetBus: EventConsumer<BottomOffset>
) : UiViewModel<OsmViewState, OsmViewEvent, OsmControllerEvent>(
    initialState = OsmViewState(
        boundingBox = null,
        loading = false,
        points = emptyList(),
        zones = emptyList(),
        centerMyLocation = null,
        nearbyError = null,
        cachedFetchError = null,
        gpsError = null,
        bottomOffset = 0
    ), debug = debug
) {

    private val mutex = Mutex()

    private val nearbyRunner = highlander<Unit, BBox?> { box ->
        setState { copy(loading = true) }

        // Run jobs in parallel
        val jobs = mutableListOf<Deferred<*>>()
        try {
            jobs += async(context = Dispatchers.Default) {
                try {
                    updateMarkers(interactor.fromCache(), fromCached = true)
                } catch (error: Throwable) {
                    error.onActualError { e ->
                        Timber.e(e, "Error getting cached supermarkets")
                        cachedFetchError(e)
                    }
                }
            }

            if (box != null) {
                jobs += async(context = Dispatchers.Default) {
                    try {
                        updateMarkers(interactor.nearbyLocations(box), fromCached = false)
                    } catch (error: Throwable) {
                        error.onActualError { e ->
                            Timber.e(e, "Error fetching nearby supermarkets")
                            nearbyError(e)
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
        doOnInit {
            initialFetchFromCache()
        }

        doOnInit {
            viewModelScope.launch(context = Dispatchers.Default) {
                bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
            }
        }
    }

    private fun nearbyError(throwable: Throwable) {
        setState { copy(nearbyError = throwable) }
    }

    private suspend fun updateMarkers(
        markers: OsmMarkers,
        fromCached: Boolean
    ) {
        mutex.withLock {
            setState {
                copy(
                    points = merge(points, markers.points) { it.id() },
                    zones = merge(zones, markers.zones) { it.id() },
                    cachedFetchError = if (fromCached) null else cachedFetchError,
                    nearbyError = if (!fromCached) null else nearbyError
                )
            }
        }
    }

    private inline fun <T : Any, ID : Any> merge(
        oldList: List<T>,
        newList: List<T>,
        id: (item: T) -> ID
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

    private fun initialFetchFromCache() {
        viewModelScope.launch(context = Dispatchers.Default) { nearbyRunner.call(null) }
    }

    private fun cachedFetchError(throwable: Throwable) {
        setState { copy(cachedFetchError = throwable) }
    }

    override fun handleViewEvent(event: OsmViewEvent) {
        return when (event) {
            is OsmViewEvent.UpdateBoundingBox -> setState { copy(boundingBox = event.box) }
            is OsmViewEvent.RequestMyLocation -> findMyLocation(event.firstTime)
            is OsmViewEvent.DoneFindingMyLocation -> doneFindingMyLocation()
            is OsmViewEvent.RequestFindNearby -> nearbySupermarkets()
        }
    }

    private fun findMyLocation(firstTime: Boolean) {
        setState {
            copy(centerMyLocation = OsmViewState.CenterMyLocation(firstTime))
        }
    }

    private fun doneFindingMyLocation() {
        setState { copy(centerMyLocation = null) }
    }

    private fun nearbySupermarkets() {
        withState {
            boundingBox?.let { box ->
                viewModelScope.launch(context = Dispatchers.Default) { nearbyRunner.call(box) }
            }
        }
    }

    fun enableGps(activity: Activity) {
        viewModelScope.launch(context = Dispatchers.Default) {
            if (!deviceGps.isGpsEnabled()) {
                try {
                    deviceGps.enableGps()
                } catch (e: Throwable) {
                    if (e is DeviceGps.ResolvableError) {
                        resolveError(e, activity)
                    } else {
                        Timber.e(e, "Error during enable GPS")
                        setState { copy(gpsError = e) }
                    }
                }
            }
        }
    }

    private suspend fun resolveError(resolution: DeviceGps.ResolvableError, activity: Activity) {
        withContext(context = Dispatchers.Main) {
            try {
                Timber.w("Resolvable error when enabling GPS, try resolve")
                resolution.resolve(activity)
            } catch (e: Throwable) {
                Timber.e(e, "Error during resolution of enable GPS error")
                setState { copy(gpsError = e) }
            }
        }
    }
}
