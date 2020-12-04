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

package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.Fridge
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.Nearby
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocationRunner @Inject internal constructor(
    private val nearby: Nearby,
    private val fridge: Fridge,
    handler: NotificationHandler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences
) : BaseRunner<LocationParameters>(
    handler,
    notificationPreferences,
    butlerPreferences,
) {

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: LocationParameters
    ) = coroutineScope {
        var closestStore: NearbyStore? = null
        var closestStoreDistance = Float.MAX_VALUE

        var closestZone: NearbyZone? = null
        var closestZoneDistance = Float.MAX_VALUE

        nearby.nearbyStores(false, RADIUS_IN_METERS).forEach { pairing ->
            Timber.d("Process nearby store: ${pairing.nearby}")
            val newClosest = if (closestStore == null) true else {
                pairing.distance < closestStoreDistance
            }

            if (newClosest) {
                Timber.d("New closest store: ${pairing.nearby}")
                closestStore = pairing.nearby
                closestStoreDistance = pairing.distance
            }
        }

        nearby.nearbyZones(false, RADIUS_IN_METERS).forEach { pairing ->
            Timber.d("Process nearby zone: ${pairing.nearby}")
            val newClosest = if (closestZone == null) true else {
                pairing.distance < closestZoneDistance
            }

            if (newClosest) {
                Timber.d("New closest zone: ${pairing.nearby}")
                closestZone = pairing.nearby
                closestZoneDistance = pairing.distance
            }
        }

        fireNotification(params, preferences, closestStore, closestZone)
    }

    @CheckResult
    private fun createResults(nearby: Boolean): NotifyResults {
        return NotifyResults(
            entryId = FridgeEntry.Id.EMPTY,
            needed = false,
            expiring = false,
            expired = false,
            nearby = nearby
        )
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

        val now = today()
        fridge.forAllItemsInEachEntry(true) { _, items ->
            val neededItems = items.filterNot { it.isArchived() }
                .filter { it.presence() == FridgeItem.Presence.NEED }
            if (neededItems.isEmpty()) {
                Timber.w("There are nearby's but nothing is needed")
                return@forAllItemsInEachEntry createResults(nearby = false)
            }

            val lastTime = preferences.getLastNotificationTimeNearby()
            if (now.isAllowedToNotify(params.forceNotifyNearby, lastTime)) {
                val storeNotified = if (storeNotification == null) false else {
                    notification { notifyNearby(storeNotification, neededItems) }
                }

                val zoneNotified = if (zoneNotification == null) false else {
                    notification { notifyNearby(zoneNotification, neededItems) }
                }

                return@forAllItemsInEachEntry createResults(nearby = storeNotified || zoneNotified)
            } else {
                return@forAllItemsInEachEntry createResults(nearby = false)
            }
        }.also { results ->
            results.firstOrNull { it.nearby }?.let {
                preferences.markNotificationNearby(now)
            }
        }
    }

    companion object {

        private const val RADIUS_IN_METERS = 1600F
    }
}
