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
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.BaseWorker
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.pydroid.ui.Injector
import timber.log.Timber

internal class GeofenceWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : BaseWorker(context, params) {

  private var geofencer: Geofencer? = null

  override fun onInject() {
    geofencer = Injector.obtain(applicationContext)
  }

  override fun onTeardown() {
    geofencer = null
  }

  override fun reschedule(butler: Butler) {
    Timber.w("Geofence jobs are not rescheduled.")
  }

  override suspend fun performWork() {
    val fences = inputData.getStringArray(KEY_FENCES) ?: emptyArray()
    if (fences.isEmpty()) {
      Timber.e("Bail: Empty fences, this should not happen!")
      return
    }

    Timber.d("Processing geofence events for fences: $fences")
    // TODO: Hit db, get nearbys. Using nearbys, match to geofence ids, show notification
  }

  companion object {

    internal const val KEY_FENCES = "key_fences"
  }

}
