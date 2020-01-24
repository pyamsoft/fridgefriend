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

package com.pyamsoft.fridge.butler.workmanager.geofence

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.NearbyNotifyingWorker
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class GeofenceNotifierWorker internal constructor(
    context: Context,
    params: WorkerParameters
) : NearbyNotifyingWorker(context, params) {

    private var geofencer: Geofencer? = null

    override fun onAfterInject() {
        geofencer = Injector.obtain(applicationContext)
    }

    override fun onAfterTeardown() {
        geofencer = null
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) {
        return coroutineScope {
            val fenceIds = inputData.getStringArray(KEY_FENCES) ?: emptyArray()
            if (fenceIds.isEmpty()) {
                Timber.e("Bail: Empty fences, this should not happen!")
                return@coroutineScope
            }
            val currentLatitude = inputData.getDouble(KEY_CURRENT_LATITUDE, BAD_COORDINATE)
            if (currentLatitude == BAD_COORDINATE) {
                Timber.e("Bail: Missing latitude, this should not happen!")
                return@coroutineScope
            }

            val currentLongitude = inputData.getDouble(KEY_CURRENT_LONGITUDE, BAD_COORDINATE)
            if (currentLongitude == BAD_COORDINATE) {
                Timber.e("Bail: Missing longitude, this should not happen!")
                return@coroutineScope
            }

            return@coroutineScope withNearbyData { stores, zones ->
                val properFenceIds = fenceIds.filterNotNull()

                var closestStore: NearbyStore? = null
                var closestStoreDistance = Float.MAX_VALUE

                var closestZone: NearbyZone? = null
                var closestZoneDistance = Float.MAX_VALUE

                for (fenceId in properFenceIds) {
                    val (store, zone) = findNearbyForGeofence(fenceId, stores, zones)

                    // Find the closest store during the loop
                    if (store != null) {
                        Timber.d("Process nearby store: $store")
                        val storeDistance =
                            store.getDistanceTo(currentLatitude, currentLongitude)
                        val newClosest = if (closestStore == null) true else {
                            storeDistance < closestStoreDistance
                        }

                        if (newClosest) {
                            Timber.d("New closest store: $store")
                            closestStore = store
                            closestStoreDistance = storeDistance
                        }
                    }

                    // Find the closest point in the closest zone during the loop
                    if (zone != null) {
                        Timber.d("Process nearby zone: $zone")
                        val zoneDistance =
                            zone.getDistanceTo(currentLatitude, currentLongitude)
                        val newClosest = if (closestZone == null) true else {
                            zoneDistance < closestZoneDistance
                        }

                        if (newClosest) {
                            Timber.d("New closest zone: $zone")
                            closestZone = zone
                            closestZoneDistance = zoneDistance
                        }
                    }
                }

                // There can be only one
                if (closestStore != null && closestZone != null) {
                    if (closestStoreDistance < closestZoneDistance) {
                        closestZone = null
                    } else {
                        closestStore = null
                    }
                }

                Timber.d("Fire notification for: $closestStore $closestZone")
                fireNotification(preferences, 0, closestStore, closestZone)
            }
        }
    }

    @CheckResult
    private fun findNearbyForGeofence(
        fenceId: String,
        nearbyStores: Collection<NearbyStore>,
        nearbyZones: Collection<NearbyZone>
    ): Nearbys {
        for (store in nearbyStores) {
            if (fenceId == Fence.getId(store)) {
                Timber.d("Geofence event $fenceId fired for zone: $store")
                return Nearbys(store, null)
            }
        }

        for (zone in nearbyZones) {
            for (point in zone.points()) {
                if (fenceId == Fence.getId(zone, point)) {
                    Timber.d("Geofence event $fenceId fired for zone point: ($point) $zone")
                    return Nearbys(null, zone)
                }
            }
        }

        Timber.w("Geofence event $fenceId fired but no matches")
        return Nearbys(null, null)
    }

    private data class Nearbys internal constructor(
        val store: NearbyStore?,
        val zone: NearbyZone?
    )

    companion object {

        private const val BAD_COORDINATE = 69420.42069
        internal const val KEY_FENCES = "key_fences"
        internal const val KEY_CURRENT_LATITUDE = "key_current_latitude"
        internal const val KEY_CURRENT_LONGITUDE = "key_current_longitude"
    }
}
