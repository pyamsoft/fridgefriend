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

package com.pyamsoft.fridge.locator.map

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.LocatorBroadcastReceiver
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GmsLocator @Inject internal constructor(
  private val context: Context
) : Locator {

  private val locationProvider = LocationServices.getFusedLocationProviderClient(context)
  private var updatePendingIntent: PendingIntent? = null
  private var locationReceiver: LocatorBroadcastReceiver? = null

  override fun hasPermission(): Boolean {
    return ContextCompat.checkSelfPermission(
        context, android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
  }

  override fun listenForUpdates(receiver: Class<out LocatorBroadcastReceiver>) {
    if (!hasPermission()) {
      Timber.w("Missing permission, return empty listener")
      return
    }

    requestLocationUpdates(receiver)
  }

  @SuppressLint("MissingPermission")
  private fun requestLocationUpdates(receiver: Class<out LocatorBroadcastReceiver>) {
    val pendingIntent =
      PendingIntent.getBroadcast(
          context, REQUEST_CODE,
          Intent(context, receiver).setAction(LocatorBroadcastReceiver.UPDATE_LISTENER_ACTION),
          PendingIntent.FLAG_UPDATE_CURRENT
      )

    val request = LocationRequest.create()
        .setInterval(INTERVAL)
        .setInterval(INTERVAL / 2)
        .setMaxWaitTime(INTERVAL * 3)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    removeLocationUpdates()

    Timber.d("Start listening for location updates")
    val broadcastReceiver = receiver.newInstance()
    context.registerReceiver(broadcastReceiver, RECEIVER_FILTER)
    locationReceiver = broadcastReceiver

    locationProvider.requestLocationUpdates(request, pendingIntent)
    updatePendingIntent = pendingIntent
  }

  private fun removeLocationUpdates() {
    updatePendingIntent?.let { locationProvider.removeLocationUpdates(it) }
    updatePendingIntent = null

    locationReceiver?.let { context.unregisterReceiver(it) }
    locationReceiver = null
  }

  override fun stopListeningForUpdates() {
    Timber.d("Stop listening for location updates")
    removeLocationUpdates()
  }

  companion object {

    private val RECEIVER_FILTER = IntentFilter(LocatorBroadcastReceiver.UPDATE_LISTENER_ACTION)
    private const val REQUEST_CODE = 1234
    private val INTERVAL = TimeUnit.SECONDS.toMillis(30)

  }

}
