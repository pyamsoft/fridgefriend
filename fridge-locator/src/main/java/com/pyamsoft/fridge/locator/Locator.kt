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

package com.pyamsoft.fridge.locator

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import java.util.concurrent.TimeUnit

interface Locator {

  @CheckResult
  fun hasBackgroundPermission(): Boolean

  @CheckResult
  fun hasForegroundPermission(): Boolean

  // If fences is empty this will throw
  fun registerGeofences(fences: List<Fence>)

  fun unregisterGeofences()

  data class Fence(
    val id: String,
    val lat: Double,
    val lon: Double
  ) {

    companion object {

      private const val STORE_ID_PREFIX = "NearbyStore v1: "
      private const val ZONE_ID_PREFIX = "NearbyStore v1: "

      @JvmStatic
      @CheckResult
      fun fromStore(store: NearbyStore): Fence {
        return Fence("$STORE_ID_PREFIX${store.id()}", store.latitude(), store.longitude())
      }

      @JvmStatic
      @CheckResult
      fun fromZone(zone: NearbyZone): List<Fence> {
        return zone.points()
            .map { fromZonePoint(zone.id(), it) }
      }

      @JvmStatic
      @CheckResult
      private fun fromZonePoint(
        zoneId: Long,
        point: NearbyZone.Point
      ): Fence {
        return Fence("$ZONE_ID_PREFIX$zoneId|${point.id}", point.lat, point.lon)
      }
    }
  }

  companion object {

    val RESCHEDULE_TIME = TimeUnit.HOURS.toMillis(3L)

  }

}
