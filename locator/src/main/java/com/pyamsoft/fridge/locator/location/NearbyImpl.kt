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

package com.pyamsoft.fridge.locator.location

import android.location.Location
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Nearby
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyImpl @Inject internal constructor(
    private val nearbyStoreQueryDao: NearbyStoreQueryDao,
    private val nearbyZoneQueryDao: NearbyZoneQueryDao,
    private val geofencer: Geofencer,
) : Nearby {

    @CheckResult
    private suspend fun getLocation(): Location? {
        return try {
            geofencer.getLastKnownLocation()
        } catch (e: Exception) {
            Timber.w("Could not get last known location - permission issue perhaps?")
            null
        }
    }

    override suspend fun nearbyStores(
        force: Boolean,
        range: Float
    ): List<Nearby.DistancePairing<NearbyStore>> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()

            val location = getLocation() ?: return@withContext emptyList<StoreDistancePairing>()
            val currentLatitude = location.latitude
            val currentLongitude = location.longitude

            return@withContext nearbyStoreQueryDao.query(force)
                .map {
                    StoreDistancePairing(
                        nearby = it,
                        distance = it.getDistanceTo(currentLatitude, currentLongitude)
                    )
                }
                .filter { it.distance <= range }
        }

    override suspend fun nearbyZones(
        force: Boolean,
        range: Float
    ): List<Nearby.DistancePairing<NearbyZone>> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()

            val location =
                getLocation() ?: return@withContext emptyList<ZoneDistancePairing>()
            val currentLatitude = location.latitude
            val currentLongitude = location.longitude

            return@withContext nearbyZoneQueryDao.query(force)
                .map {
                    ZoneDistancePairing(
                        nearby = it,
                        distance = it.getDistanceTo(currentLatitude, currentLongitude)
                    )
                }
                .filter { it.distance <= range }
        }

    private data class StoreDistancePairing(
        override val nearby: NearbyStore,
        override val distance: Float
    ) : Nearby.DistancePairing<NearbyStore>

    private data class ZoneDistancePairing(
        override val nearby: NearbyZone,
        override val distance: Float
    ) : Nearby.DistancePairing<NearbyZone>

    companion object {

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

    }

}