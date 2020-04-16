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

import android.location.Location
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.runner.NearbyRunner
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

internal class LocationRunner @Inject internal constructor(
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao,
    storeDb: NearbyStoreQueryDao,
    zoneDb: NearbyZoneQueryDao,
    private val geofencer: Geofencer
) : NearbyRunner<LocationParameters>(
    handler,
    butler,
    notificationPreferences,
    butlerPreferences,
    enforcer,
    fridgeEntryQueryDao,
    fridgeItemQueryDao,
    storeDb,
    zoneDb
) {
    override suspend fun reschedule(butler: Butler, params: LocationParameters) {
        butler.scheduleRemindLocation(
            LocationParameters(
                forceNotifyNeeded = false
            )
        )
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: LocationParameters
    ) = coroutineScope {
        val location = try {
            geofencer.getLastKnownLocation()
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

            stores.forEach { store ->
                val storeDistance =
                    store.getDistanceTo(currentLatitude, currentLongitude)
                if (storeDistance > RADIUS_IN_METERS) {
                    // Out of range
                    return@forEach
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

            zones.forEach { zone ->
                val zoneDistance =
                    zone.getDistanceTo(currentLatitude, currentLongitude)

                if (zoneDistance > RADIUS_IN_METERS) {
                    // Out of range
                    return@forEach
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

            fireNotification(params, preferences, closestStore, closestZone)
        }
    }

    private suspend fun fireNotification(
        params: LocationParameters,
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
                .filter { it.presence() == FridgeItem.Presence.NEED }
            if (neededItems.isEmpty()) {
                Timber.w("There are nearby's but nothing is needed")
                return@withFridgeData
            }

            val now = today()
            val lastTime = preferences.getLastNotificationTimeNearby()
            if (now.isAllowedToNotify(params.forceNotifyNeeded, lastTime)) {
                if (storeNotification != null) {
                    notification { handler ->
                        if (handler.notifyNearby(storeNotification, neededItems)) {
                            preferences.markNotificationNearby(now)
                        }
                    }
                }

                if (zoneNotification != null) {
                    notification { handler ->
                        if (handler.notifyNearby(zoneNotification, neededItems)) {
                            preferences.markNotificationNearby(now)
                        }
                    }
                }
            }
        }
    }

    // Get the closest point in the zone to the lat lon
    @CheckResult
    private fun NearbyZone.getDistanceTo(lat: Double, lon: Double): Float {
        var closestDistance = Float.MAX_VALUE
        this.points().forEach { point ->
            val distance = getDistance(point.lat, point.lon, lat, lon)
            if (distance < closestDistance) {
                closestDistance = distance
            }
        }

        return closestDistance
    }

    @CheckResult
    private fun NearbyStore.getDistanceTo(lat: Double, lon: Double): Float {
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

    companion object {

        private const val RADIUS_IN_METERS = 1600F
    }
}
