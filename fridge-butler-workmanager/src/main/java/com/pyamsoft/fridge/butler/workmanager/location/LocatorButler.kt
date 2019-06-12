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

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.pyamsoft.fridge.butler.Locator
import com.pyamsoft.fridge.butler.Locator.LastKnownLocation
import com.pyamsoft.fridge.butler.Locator.MissingLocationPermissionException
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import io.reactivex.SingleEmitter
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocatorButler @Inject internal constructor(
  private val context: Context,
  private val enforcer: Enforcer
) : Locator {

  private val locationProvider = LocationServices.getFusedLocationProviderClient(context)

  override fun hasPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun lastKnownLocation(): Single<LastKnownLocation> {
    return Single.create { emitter ->
      enforcer.assertNotOnMainThread()

      if (!hasPermission()) {
        emitter.tryOnError(MissingLocationPermissionException)
      } else {
        getLastKnownLocation(emitter)
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun getLastKnownLocation(emitter: SingleEmitter<LastKnownLocation>) {
    locationProvider.lastLocation
        .addOnSuccessListener { location ->
          enforcer.assertNotOnMainThread()

          if (!emitter.isDisposed) {
            if (location == null) {
              Timber.w("Last known location is unknown")
              emitter.onSuccess(LastKnownLocation.UNKNOWN)
            } else {
              Timber.d("Last known location: $location")
              emitter.onSuccess(LastKnownLocation(location))
            }
          }
        }
        .addOnFailureListener {
          enforcer.assertNotOnMainThread()

          Timber.e(it, "Failed to get last known location")
          emitter.tryOnError(it)
        }
  }

}
