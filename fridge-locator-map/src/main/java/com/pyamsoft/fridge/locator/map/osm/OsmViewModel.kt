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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.BackgroundPermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.StoragePermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.FindNearby
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestBackgroundPermission
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestStoragePermission
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject

class OsmViewModel @Inject internal constructor(
    private val interactor: OsmInteractor
) : UiViewModel<OsmViewState, OsmViewEvent, OsmControllerEvent>(
    initialState = OsmViewState(
        loading = false, points = emptyList(), zones = emptyList(), nearbyError = null,
        cachedFetchError = null
    )
) {

    private val mutex = Mutex()

    private val nearbyRunner = highlander<Unit, BBox> { box ->
        setState { copy(loading = true) }

        // Run jobs in parallel
        try {
            launch {
                try {
                    updateMarkers(interactor.fromCache(), fromCached = true)
                } catch (error: Throwable) {
                    error.onActualError { e ->
                        Timber.e(e, "Error getting cached supermarkets")
                        cachedFetchError(e)
                    }
                }
            }

            launch {
                try {
                    updateMarkers(interactor.nearbyLocations(box), fromCached = false)
                } catch (error: Throwable) {
                    error.onActualError { e ->
                        Timber.e(e, "Error fetching nearby supermarkets")
                        nearbyError(e)
                    }
                }
            }
        } finally {
            setState { copy(loading = false) }
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
                if (fromCached) {
                    copy(
                        points = merge(points, markers.points) { it.id() },
                        zones = merge(zones, markers.zones) { it.id() },
                        cachedFetchError = null
                    )
                } else {
                    copy(
                        points = merge(points, markers.points) { it.id() },
                        zones = merge(zones, markers.zones) { it.id() },
                        nearbyError = null
                    )
                }
            }
        }
    }

    private inline fun <T : Any> merge(
        oldList: List<T>,
        newList: List<T>,
        id: (item: T) -> Long
    ): List<T> {
        val result = ArrayList(newList)
        for (oldItem in oldList) {
            val oldId = id(oldItem)
            // If the new list doesn't have the old id, add the old id
            // Otherwise, the new item is newer.
            if (result.find { id(it) == oldId } == null) {
                result.add(oldItem)
            }
        }
        return result
    }

    override fun onInit() {
        initialFetchFromCache()
    }

    private fun initialFetchFromCache() {
        viewModelScope.launch {
            setState { copy(loading = true) }
            try {
                updateMarkers(interactor.fromCache(), fromCached = true)
            } catch (error: Throwable) {
                error.onActualError { e ->
                    Timber.e(e, "Error getting cached locations")
                    cachedFetchError(e)
                }
            } finally {
                setState { copy(loading = false) }
            }
        }
    }

    private fun cachedFetchError(throwable: Throwable) {
        setState { copy(cachedFetchError = throwable) }
    }

    override fun handleViewEvent(event: OsmViewEvent) {
        return when (event) {
            is FindNearby -> nearbySupermarkets(event.box)
            is RequestBackgroundPermission -> publish(BackgroundPermissionRequest)
            is RequestStoragePermission -> handleStorageRequestEvent(manual = true)
        }
    }

    private fun nearbySupermarkets(box: BBox) {
        viewModelScope.launch { nearbyRunner.call(box) }
    }

    fun requestStoragePermission() {
        handleStorageRequestEvent(manual = false)
    }

    private fun handleStorageRequestEvent(manual: Boolean) {
        if (manual) {
            // Manual requests are always honored
            Timber.d("STORAGE permission manually requested")
            publish(StoragePermissionRequest)
            return
        }

        viewModelScope.launch {
            // TODO Check preferences to see if STORAGE was already requested
            val alreadyRequested = false

            if (!alreadyRequested) {
                // Do not launch this in a Scope because we want it to finish before continuing.
                // If we do not wait, potentially a double click event could fire the request twice.
                // TODO Write preferences marking STORAGE as already requested

                Timber.d("Request STORAGE permission on behalf of first-time user")
                publish(StoragePermissionRequest)
            }
        }
    }
}
