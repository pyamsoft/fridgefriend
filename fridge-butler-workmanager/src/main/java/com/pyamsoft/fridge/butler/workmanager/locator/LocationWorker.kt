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

package com.pyamsoft.fridge.butler.workmanager.locator

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.NearbyNotifyingWorker
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

internal class LocationWorker internal constructor(
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

    override fun reschedule(butler: Butler) {
        butler.remindLocation(RECURRING_INTERVAL, HOURS)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) = coroutineScope {
        val location = try {
            requireNotNull(geofencer).getLastKnownLocation()
        } catch (e: Exception) {
            Timber.w("Could not get last known location - permission issue perhaps?")
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

            fireNotification(preferences, RECURRING_INTERVAL, closestStore, closestZone)
        }
    }

    companion object {

        private const val RECURRING_INTERVAL = 3L
    }
}
