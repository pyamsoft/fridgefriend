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
import android.location.Location
import androidx.annotation.CheckResult
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.worker.NearbyNotifyingWorker
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
        butler.remindLocation(3, HOURS)
    }

    override suspend fun performWork() = coroutineScope {
        val location = requireNotNull(geofencer).getLastKnownLocation()

        if (location == null) {
            Timber.w("Last Known location was null, cannot continue")
            return@coroutineScope
        }

        withNearbyData { stores, zones ->
            val inRangeStores = mutableSetOf<NearbyStore>()
            val inRangeZones = mutableSetOf<NearbyZone>()

            for (store in stores) {
                val storeLocation = fromLatLong(store.latitude(), store.longitude())
                if (location.distanceTo(storeLocation) <= Locator.RADIUS_IN_METERS) {
                    inRangeStores.add(store)
                }
            }

            for (zone in zones) {
                for (point in zone.points()) {
                    val pointLocation = fromLatLong(point.lat, point.lon)
                    if (location.distanceTo(pointLocation) <= Locator.RADIUS_IN_METERS) {
                        inRangeZones.add(zone)
                        break
                    }
                }
            }

            fireNotifications(inRangeStores, inRangeZones)
        }
    }

    companion object {

        @JvmStatic
        @CheckResult
        private fun fromLatLong(
            lat: Double,
            lon: Double
        ): Location {
            return Location(Locator.PROVIDER).apply {
                latitude = lat
                longitude = lon
            }
        }
    }
}
