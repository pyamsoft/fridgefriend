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

package com.pyamsoft.fridge.butler.workmanager.geofence

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.FridgeWorker
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class GeofenceWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : FridgeWorker(context, params) {

  private var geofencer: Geofencer? = null
  private var storeDb: NearbyStoreQueryDao? = null
  private var zoneDb: NearbyZoneQueryDao? = null

  override fun afterInject() {
    geofencer = Injector.obtain(applicationContext)
    storeDb = Injector.obtain(applicationContext)
    zoneDb = Injector.obtain(applicationContext)
  }

  override fun afterTeardown() {
    geofencer = null
    storeDb = null
    zoneDb = null
  }

  override fun reschedule(butler: Butler) {
    Timber.w("Geofence jobs are not rescheduled.")
  }

  override suspend fun performWork() {
    return coroutineScope {
      val fenceIds = inputData.getStringArray(KEY_FENCES) ?: emptyArray()
      if (fenceIds.isEmpty()) {
        Timber.e("Bail: Empty fences, this should not happen!")
        return@coroutineScope
      }

      Timber.d("Processing geofence events for fences")
      val storeJob = async { requireNotNull(storeDb).query(false) }
      val zoneJob = async { requireNotNull(zoneDb).query(false) }

      val nearbyStores = storeJob.await()
      val nearbyZones = zoneJob.await()
      val properFenceIds = fenceIds.filterNotNull()

      val storeNotifications = mutableSetOf<NearbyStore>()
      val zoneNotifications = mutableSetOf<NearbyZone>()
      for (fenceId in properFenceIds) {
        val (store, zone) = findNearbyForGeofence(fenceId, nearbyStores, nearbyZones)
        if (store != null) {
          storeNotifications.add(store)
        }

        if (zone != null) {
          zoneNotifications.add(zone)
        }
      }

      fireNotifications(storeNotifications, zoneNotifications)
    }
  }

  private suspend fun fireNotifications(
    storeNotifications: Set<NearbyStore>,
    zoneNotifications: Set<NearbyZone>
  ) {
    if (storeNotifications.isEmpty() && zoneNotifications.isEmpty()) {
      Timber.w("Cannot process a completely empty fence event")
      return
    }

    withFridgeData { entry, items ->
      val neededItems = items.filterNot { it.isArchived() }
          .filter { it.presence() == NEED }
      if (neededItems.isEmpty()) {
        Timber.w("There are nearby's but nothing is needed")
        return@withFridgeData
      }

      storeNotifications.forEach { store ->
        notification { handler, foregroundState ->
          GeofenceNotifications.notifyNeeded(
              handler, foregroundState, applicationContext, entry, store, neededItems
          )
        }
      }
      zoneNotifications.forEach { zone ->
        notification { handler, foregroundState ->
          GeofenceNotifications.notifyNeeded(
              handler, foregroundState, applicationContext, entry, zone, neededItems
          )
        }
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
      if (fenceId == Fence.getId(store)) {
        Timber.d("Geofence event $fenceId fired for zone: $store")
        return Nearbys(store, null)
      }
    }

    for (zone in nearbyZones) {
      for (point in zone.points()) {
        if (fenceId == Fence.getId(zone, point)) {
          Timber.d("Geofence event $fenceId fired for zone point: ($point) $zone")
          return Nearbys(null, zone)
        }
      }
    }

    Timber.w("Geofence event $fenceId fired but no matches")
    return Nearbys(null, null)
  }

  private data class Nearbys internal constructor(
    val store: NearbyStore?,
    val zone: NearbyZone?
  )

  companion object {

    internal const val KEY_FENCES = "key_fences"
  }

}
