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

package com.pyamsoft.fridge.butler.runner

import android.content.Context
import android.location.Location
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.runner.geofence.GeofenceNotifications
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import java.util.Calendar

internal abstract class NearbyNotifyingRunner protected constructor(
    private val context: Context,
    handler: NotificationHandler,
    butler: Butler,
    butlerPreferences: ButlerPreferences,
    fridgeItemPreferences: FridgeItemPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao,
    storeDb: NearbyStoreQueryDao,
    zoneDb: NearbyZoneQueryDao
) : NearbyRunner(
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

    protected suspend fun fireNotification(
        forceNotify: Boolean,
        preferences: ButlerPreferences,
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
            val lastTime = preferences.getLastNotificationTimeNearby()
            val allowed = forceNotify || now.isAllowedToNotify(lastTime)
            if (allowed) {
                if (storeNotification != null) {
                    notification { handler ->
                        Timber.d("Fire notification for: $storeNotification")
                        val notified = GeofenceNotifications.notifyNeeded(
                            handler,
                            context.applicationContext,
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
                            context.applicationContext,
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
