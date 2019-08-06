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
import com.pyamsoft.fridge.butler.workmanager.BaseWorker
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

internal class LocatorWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : BaseWorker(context, params) {

  private var locator: Locator? = null
  private var storeDb: NearbyStoreQueryDao? = null
  private var zoneDb: NearbyZoneQueryDao? = null

  override fun onInject() {
    locator = Injector.obtain(applicationContext)
    storeDb = Injector.obtain(applicationContext)
    zoneDb = Injector.obtain(applicationContext)
  }

  override fun onTeardown() {
    locator = null
    storeDb = null
    zoneDb = null
  }

  override fun reschedule(butler: Butler) {
    butler.remindLocation(Locator.RESCHEDULE_TIME, HOURS)
  }

  override suspend fun performWork() {
    return coroutineScope {
      Timber.d("LocatorWorker registering fences")

      val storeJob = async { requireNotNull(storeDb).query(true) }
      val zoneJob = async { requireNotNull(zoneDb).query(true) }

      val nearbyStores = storeJob.await()
          .map { store ->
            Timber.d("Geofencing nearby zone: $store")
            return@map Fence.fromStore(store)
          }
      val nearbyZones = zoneJob.await()
          .map { zone ->
            Timber.d("Geofencing nearby zone: $zone")
            return@map Fence.fromZone(zone)
          }
          .flatten()

      val fences = nearbyStores + nearbyZones
      if (fences.isEmpty()) {
        Timber.w("List of fences is empty - ignore this request")
        return@coroutineScope
      }

      Timber.d("Attempting to register fences: $fences")
      requireNotNull(locator).registerGeofences(fences)
    }
  }
}
