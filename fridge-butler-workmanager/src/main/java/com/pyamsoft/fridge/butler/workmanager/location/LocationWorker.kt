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

package com.pyamsoft.fridge.butler.workmanager.location

import android.content.Context
import android.location.Location
import androidx.annotation.CheckResult
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.Locator
import com.pyamsoft.fridge.butler.Locator.LastKnownLocation
import com.pyamsoft.fridge.butler.Locator.MissingLocationPermissionException
import com.pyamsoft.fridge.butler.workmanager.BaseWorker
import com.pyamsoft.pydroid.ui.Injector
import io.reactivex.Single
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

internal class LocationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : BaseWorker(context, params) {

  private var locator: Locator? = null

  override fun onInject() {
    locator = Injector.obtain(applicationContext)
  }

  override fun onTeardown() {
    locator = null
  }

  override fun reschedule(butler: Butler) {
    butler.cancelLocationReminder()
    butler.remindLocation(1L, HOURS)
  }

  override fun doWork(): Single<*> {
    enforcer.assertNotOnMainThread()

    return requireNotNull(locator).lastKnownLocation()
        .subscribeOn(backgroundScheduler)
        .observeOn(backgroundScheduler)
        .onErrorResumeNext { throwable: Throwable ->
          Timber.e(throwable, "Error during location reminder work")
          if (throwable is MissingLocationPermissionException) {
            // If its a missing permission error thats okay, swallow it.
            return@onErrorResumeNext Single.just(LastKnownLocation.UNKNOWN)
          } else {
            return@onErrorResumeNext Single.error(throwable)
          }
        }
        .filter { it.location != null }
        .map { requireNotNull(it.location) }
        .flatMapSingle { handleLastKnownLocation(it) }
  }

  @CheckResult
  private fun handleLastKnownLocation(location: Location): Single<*> {
    return Single.defer {
      enforcer.assertNotOnMainThread()

      Timber.d("Compare last known location to stores nearby: $location")
      // TODO(peter): Network call to get all markets in a 1 mile radius
      // Compare distances with location.distanceTo()
      // val storeLat = network.latitude
      // val storeLon = network.longitude
      // val storeLocation = Location(LocationManager.GPS_PROVIDER).apply {
      //   latitude = storeLat
      //   longitude = storeLon
      // }
      // val distanceToStoreInMeters = location.distanceTo(storeLocation)
      return@defer Single.just(Unit)
          .subscribeOn(backgroundScheduler)
          .observeOn(backgroundScheduler)
    }
        .subscribeOn(backgroundScheduler)
        .observeOn(backgroundScheduler)
  }
}
