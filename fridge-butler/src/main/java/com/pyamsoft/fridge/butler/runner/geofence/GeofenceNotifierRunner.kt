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
 *
 */

package com.pyamsoft.fridge.butler.runner.geofence

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.runner.NearbyNotifyingRunner
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class GeofenceNotifierRunner @Inject internal constructor(
    context: Context,
    handler: NotificationHandler,
    butler: Butler,
    butlerPreferences: ButlerPreferences,
    fridgeItemPreferences: FridgeItemPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao,
    storeDb: NearbyStoreQueryDao,
    zoneDb: NearbyZoneQueryDao,
    private val fenceIds: Array<out String>,
    @Named("latitude") private val latitude: Double?,
    @Named("longitude") private val longitude: Double?
) : NearbyNotifyingRunner(
    context,
    handler,
    butler,
    butlerPreferences,
    fridgeItemPreferences,
    enforcer,
    fridgeEntryQueryDao,
    fridgeItemQueryDao,
    storeDb,
    zoneDb
) {

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) {
        val fences = fenceIds
        val currentLatitude = latitude
        val currentLongitude = longitude
        return coroutineScope {

            if (fences.isEmpty()) {
                Timber.e("Bail: Empty fences, this should not happen!")
                return@coroutineScope
            }

            if (currentLatitude == null) {
                Timber.e("Bail: Missing latitude, this should not happen!")
                return@coroutineScope
            }

            if (currentLongitude == null) {
                Timber.e("Bail: Missing longitude, this should not happen!")
                return@coroutineScope
            }

            return@coroutineScope withNearbyData { stores, zones ->
                var closestStore: NearbyStore? = null
                var closestStoreDistance = Float.MAX_VALUE

                var closestZone: NearbyZone? = null
                var closestZoneDistance = Float.MAX_VALUE

                for (fenceId in fences) {
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
                fireNotification(true, preferences, closestStore, closestZone)
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
            if (fenceId == Locator.Fence.getId(store)) {
                Timber.d("Geofence event $fenceId fired for zone: $store")
                return Nearbys(
                    store,
                    null
                )
            }
        }

        for (zone in nearbyZones) {
            for (point in zone.points()) {
                if (fenceId == Locator.Fence.getId(zone, point)) {
                    Timber.d("Geofence event $fenceId fired for zone point: ($point) $zone")
                    return Nearbys(
                        null,
                        zone
                    )
                }
            }
        }

        Timber.w("Geofence event $fenceId fired but no matches")
        return Nearbys(
            null,
            null
        )
    }

    private data class Nearbys internal constructor(
        val store: NearbyStore?,
        val zone: NearbyZone?
    )
}
