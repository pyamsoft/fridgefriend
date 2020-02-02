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

package com.pyamsoft.fridge.butler.runner.locator

import android.content.Context
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.runner.NearbyNotifyingRunner
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class LocationRunner @Inject internal constructor(
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
    private val geofencer: Geofencer
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

    override fun reschedule(butler: Butler) {
        butler.scheduleRemindLocation()
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) = coroutineScope {
        val location = try {
            geofencer.getLastKnownLocation()
        } catch (e: Exception) {
            Timber.w("Could not get last known location - permission_button issue perhaps?")
            null
        }

        if (location == null) {
            Timber.w("Last Known location was null, cannot continue")
            return@coroutineScope
        }

        var closestStore: NearbyStore? = null
        var closestStoreDistance = Float.MAX_VALUE

        var closestZone: NearbyZone? = null
        var closestZoneDistance = Float.MAX_VALUE

        return@coroutineScope withNearbyData { stores, zones ->
            val currentLatitude = location.latitude
            val currentLongitude = location.longitude

            for (store in stores) {
                val storeDistance =
                    store.getDistanceTo(currentLatitude, currentLongitude)
                if (storeDistance > Locator.RADIUS_IN_METERS) {
                    // Out of range
                    continue
                }

                Timber.d("Process nearby store: $store")
                val newClosest = if (closestStore == null) true else {
                    storeDistance < closestStoreDistance
                }

                if (newClosest) {
                    Timber.d("New closest store: $store")
                    closestStore = store
                    closestStoreDistance = storeDistance
                }
            }

            for (zone in zones) {
                val zoneDistance =
                    zone.getDistanceTo(currentLatitude, currentLongitude)

                if (zoneDistance > Locator.RADIUS_IN_METERS) {
                    // Out of range
                    continue
                }

                Timber.d("Process nearby zone: $zone")
                val newClosest = if (closestZone == null) true else {
                    zoneDistance < closestZoneDistance
                }

                if (newClosest) {
                    Timber.d("New closest zone: $zone")
                    closestZone = zone
                    closestZoneDistance = zoneDistance
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

            fireNotification(false, preferences, closestStore, closestZone)
        }
    }
}
