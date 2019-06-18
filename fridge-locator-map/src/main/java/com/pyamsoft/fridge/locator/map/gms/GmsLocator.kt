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

package com.pyamsoft.fridge.locator.map.gms

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.pyamsoft.fridge.locator.LastKnownLocation
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.LocatorBroadcastReceiver
import com.pyamsoft.fridge.locator.MissingLocationPermissionException
import com.pyamsoft.fridge.locator.map.R.string
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import io.reactivex.SingleEmitter
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GmsLocator @Inject internal constructor(
  private val receiverClass: Class<out LocatorBroadcastReceiver>,
  private val enforcer: Enforcer,
  private val context: Context
) : Locator {

  private val lock = Any()

  private val locationProvider = LocationServices.getFusedLocationProviderClient(context)
  @Volatile private var updatePendingIntent: PendingIntent? = null

  override fun hasPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun listenForUpdates() {
    if (!hasPermission()) {
      Timber.w("Missing permission, return empty listener")
      return
    }

    requestLocationUpdates()
  }

  @SuppressLint("MissingPermission")
  private fun requestLocationUpdates() {
    val action = context.getString(
        string.locator_broadcast_receiver_action
    )
    val pendingIntent =
      PendingIntent.getBroadcast(
          context, REQUEST_CODE,
          Intent(context, receiverClass).setAction(action),
          PendingIntent.FLAG_UPDATE_CURRENT
      )

    val request = LocationRequest.create()
        .setInterval(INTERVAL)
        .setInterval(INTERVAL / 2)
        .setMaxWaitTime(INTERVAL * 3)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    synchronized(lock) {
      removeLocationUpdates()

      Timber.d("Start listening for location updates")
      locationProvider.requestLocationUpdates(request, pendingIntent)
      updatePendingIntent = pendingIntent
    }
  }

  private fun removeLocationUpdates() {
    updatePendingIntent?.let { locationProvider.removeLocationUpdates(it) }
    updatePendingIntent = null
  }

  override fun stopListeningForUpdates() {
    Timber.d("Stop listening for location updates")
    synchronized(lock) {
      removeLocationUpdates()
    }
  }

  override fun getLastKnownLocation(): Single<LastKnownLocation> {
    return Single.create { emitter ->
      enforcer.assertNotOnMainThread()

      if (!hasPermission()) {
        emitter.tryOnError(MissingLocationPermissionException)
      } else {
        emitLastKnownLocation(emitter)
      }
    }
  }

  @SuppressLint("MissingPermission")
  private fun emitLastKnownLocation(emitter: SingleEmitter<LastKnownLocation>) {
    locationProvider.lastLocation.addOnSuccessListener { location ->
      // Android runs this callback OMT
      if (emitter.isDisposed) {
        Timber.w("Emitter is already disposed")
        return@addOnSuccessListener
      }

      if (location == null) {
        Timber.w("Last known location is unknown")
        emitter.onSuccess(LastKnownLocation(null))
      } else {
        Timber.d("Last known location is $location")
        emitter.onSuccess(LastKnownLocation(location))
      }
    }
        .addOnFailureListener {
          // Android runs this callback OMT
          Timber.e(it, "Failed to get last known location")
          emitter.tryOnError(it)
        }
  }

  companion object {

    private const val REQUEST_CODE = 1234
    private val INTERVAL = TimeUnit.SECONDS.toMillis(30)

  }

}
