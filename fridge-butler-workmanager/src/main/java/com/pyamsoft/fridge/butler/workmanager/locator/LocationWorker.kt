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
import com.pyamsoft.fridge.butler.workmanager.FridgeWorker
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class LocationWorker internal constructor(
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
    Timber.w("Location jobs are not rescheduled.")
  }

  override suspend fun performWork() {
    return coroutineScope {
      val latitude = inputData.getDouble(KEY_LATITUDE, DEFAULT_LOCATION_COORDINATE)
      val longitude = inputData.getDouble(KEY_LONGITUDE, DEFAULT_LOCATION_COORDINATE)

      if (latitude == DEFAULT_LOCATION_COORDINATE || longitude == DEFAULT_LOCATION_COORDINATE) {
        Timber.w("Latitude or Longitude were not provided to worker")
        return@coroutineScope
      }

      Timber.d("Last known location: ($latitude, $longitude)")
    }
  }

  companion object {

    private const val DEFAULT_LOCATION_COORDINATE = -1000000.0
    internal const val KEY_LATITUDE = "key_latitude"
    internal const val KEY_LONGITUDE = "key_longitude"
  }

}
