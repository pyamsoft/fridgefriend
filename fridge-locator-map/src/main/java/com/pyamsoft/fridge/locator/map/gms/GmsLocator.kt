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
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.pyamsoft.fridge.locator.LastKnownLocation
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.LocatorBroadcastReceiver
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
internal class GmsLocator @Inject internal constructor(
  private val receiverClass: Class<out LocatorBroadcastReceiver>,
  private val enforcer: Enforcer,
  private val context: Context
) : Locator {

  private val lock = Any()

  private val locationProvider = LocationServices.getFusedLocationProviderClient(context)
  @Volatile private var updatePendingIntent: PendingIntent? = null

  override fun hasForegroundPermission(): Boolean {
    val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
    val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
    return permissionCheck == PackageManager.PERMISSION_GRANTED
  }

  override fun hasBackgroundPermission(): Boolean {
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
      val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
      return permissionCheck == PackageManager.PERMISSION_GRANTED
    } else {
      return true
    }
  }

  override fun listenForUpdates() {
    if (!hasForegroundPermission()) {
      val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
      Timber.w("Missing $permission permission, return empty listener")
      return
    }

    if (!hasBackgroundPermission()) {
      if (VERSION.SDK_INT >= VERSION_CODES.Q) {
        val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        Timber.w("Missing $permission permission, return empty listener")
      }
      return
    }

    requestLocationUpdates()
  }

  private fun requestLocationUpdates() {

    synchronized(lock) {
      removeLocationUpdates()

      if (!hasForegroundPermission()) {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        Timber.w("Missing $permission permission, cannot request updates")
        return
      }

      if (!hasBackgroundPermission()) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
          val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
          Timber.w("Missing $permission permission, cannot request updates")
        }
        return
      }

      startLocationUpdates()
    }
  }

  @SuppressLint("MissingPermission")
  private fun startLocationUpdates() {
    Timber.d("Start listening for location updates")

    val action = context.getString(R.string.locator_broadcast_receiver_action)
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

    locationProvider.requestLocationUpdates(request, pendingIntent)
    updatePendingIntent = pendingIntent
  }

  private fun removeLocationUpdates() {
    updatePendingIntent?.let {
      Timber.d("Stop listening for location updates")
      locationProvider.removeLocationUpdates(it)
    }
    updatePendingIntent = null
  }

  override fun stopListeningForUpdates() {
    synchronized(lock) {
      removeLocationUpdates()
    }
  }

  override suspend fun getLastKnownLocation(): LastKnownLocation {
    return listenForLastKnownLocation()
  }

  private suspend fun listenForLastKnownLocation(): LastKnownLocation =
    suspendCoroutine { continuation ->
      enforcer.assertNotOnMainThread()
      if (!hasForegroundPermission()) {
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        Timber.w("Missing $permission permission for last known location")
        continuation.resume(LastKnownLocation(null))
        return@suspendCoroutine
      }

      if (!hasBackgroundPermission()) {
        if (VERSION.SDK_INT >= VERSION_CODES.Q) {
          val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
          Timber.w("Missing $permission permission for last known location")
        }
        continuation.resume(LastKnownLocation(null))
        return@suspendCoroutine
      }

      emitLastKnownLocation(continuation)
    }

  @SuppressLint("MissingPermission")
  private fun emitLastKnownLocation(continuation: Continuation<LastKnownLocation>) {
    locationProvider.lastLocation.addOnSuccessListener { location ->
      // Android runs this callback OMT
      if (location == null) {
        Timber.w("Last known location is unknown")
      } else {
        Timber.d("Last known location is $location")
      }
      continuation.resume(LastKnownLocation(location))
    }
        .addOnFailureListener {
          // Android runs this callback OMT
          Timber.e(it, "Failed to get last known location")
          continuation.resumeWithException(it)
        }
  }

  companion object {

    private const val REQUEST_CODE = 1234
    private val INTERVAL = TimeUnit.SECONDS.toMillis(30)

  }

}
