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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import android.location.Location
import androidx.annotation.CheckResult
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.workmanager.geofence.GeofenceNotifications
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import java.util.Calendar
import timber.log.Timber

internal abstract class NearbyNotifyingWorker protected constructor(
    context: Context,
    params: WorkerParameters
) : NearbyWorker(context, params) {

    protected suspend fun fireNotification(
        preferences: ButlerPreferences,
        rescheduleInterval: Long,
        storeNotification: NearbyStore?,
        zoneNotification: NearbyZone?
    ) {
        if (storeNotification == null && zoneNotification == null) {
            Timber.w("Cannot process a completely empty event")
            return
        } else if (storeNotification != null && zoneNotification != null) {
            Timber.w("Cannot process an event with both Store and Zone")
            return
        }

        withFridgeData { _, items ->
            val neededItems = items.filterNot { it.isArchived() }
                .filter { it.presence() == NEED }
            if (neededItems.isEmpty()) {
                Timber.w("There are nearby's but nothing is needed")
                return@withFridgeData
            }

            val now = Calendar.getInstance()
            val allowed = now.isAllowedToNotify(
                preferences.getLastNotificationTimeNearby(),
                rescheduleInterval
            )
            if (allowed) {
                if (storeNotification != null) {
                    notification { handler ->
                        Timber.d("Fire notification for: $storeNotification")
                        val notified = GeofenceNotifications.notifyNeeded(
                            handler,
                            applicationContext,
                            storeNotification,
                            now,
                            neededItems
                        )
                        if (notified) {
                            preferences.markNotificationNearby(now)
                        }
                    }
                }

                if (zoneNotification != null) {
                    notification { handler ->
                        Timber.d("Fire notification for: $zoneNotification")
                        val notified = GeofenceNotifications.notifyNeeded(
                            handler,
                            applicationContext,
                            zoneNotification,
                            now,
                            neededItems
                        )
                        if (notified) {
                            preferences.markNotificationNearby(now)
                        }
                    }
                }
            }
        }
    }

    // Get the closest point in the zone to the lat lon
    @CheckResult
    protected fun NearbyZone.getDistanceTo(lat: Double, lon: Double): Float {
        var closestDistance = Float.MAX_VALUE
        for (point in this.points()) {
            val distance = getDistance(point.lat, point.lon, lat, lon)
            if (distance < closestDistance) {
                closestDistance = distance
            }
        }

        return closestDistance
    }

    @CheckResult
    protected fun NearbyStore.getDistanceTo(lat: Double, lon: Double): Float {
        return getDistance(this.latitude(), this.longitude(), lat, lon)
    }

    @CheckResult
    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val resultArray = FloatArray(1)
        Location.distanceBetween(
            lat1,
            lon1,
            lat2,
            lon2,
            resultArray
        )

        // Arbitrary Android magic number
        // https://developer.android.com/reference/android/location/Location.html#distanceBetween(double,%20double,%20double,%20double,%20float%5B%5D)
        return resultArray[0]
    }
}
