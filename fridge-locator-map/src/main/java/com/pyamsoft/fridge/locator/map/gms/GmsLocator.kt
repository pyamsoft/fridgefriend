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
import androidx.annotation.CheckResult
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.pyamsoft.fridge.locator.GeofenceBroadcastReceiver
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GmsLocator @Inject internal constructor(
  receiverClass: Class<out GeofenceBroadcastReceiver>,
  private val enforcer: Enforcer,
  private val context: Context
) : Locator, MapPermission {

  private val client = LocationServices.getGeofencingClient(context)
  private val pendingIntent = PendingIntent.getBroadcast(
      context, REQUEST_CODE, Intent(context, receiverClass), PendingIntent.FLAG_UPDATE_CURRENT
  )

  @CheckResult
  private fun checkPermission(permission: String): Boolean {
    return checkPermission(permission)
  }

  @CheckResult
  private fun checkPermission(vararg permissions: String): Boolean {
    return permissions.all { permission ->
      val permissionCheck = ContextCompat.checkSelfPermission(context, permission)
      return permissionCheck == PackageManager.PERMISSION_GRANTED
    }
  }

  private fun requestPermission(
    fragment: Fragment,
    requestCode: Int,
    permission: String
  ) {
    requestPermission(fragment, requestCode, permission)
  }

  private fun requestPermission(
    fragment: Fragment,
    requestCode: Int,
    vararg permissions: String
  ) {
    fragment.requestPermissions(permissions, requestCode)
  }

  private inline fun onPermissionResult(
    requestCode: Int,
    expectedCode: Int,
    hasPermission: () -> Boolean,
    onPermissionGranted: () -> Unit
  ) {
    if (requestCode == expectedCode) {
      if (hasPermission()) {
        onPermissionGranted()
      }
    }
  }

  override fun hasForegroundPermission(): Boolean {
    return checkPermission(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
  }

  override fun requestForegroundPermission(fragment: Fragment) {
    requestPermission(
        fragment,
        FOREGROUND_LOCATION_PERMISSION_REQUEST_RC,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )
  }

  override fun onForegroundResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    onForegroundPermissionGranted: () -> Unit
  ) {
    onPermissionResult(
        requestCode, FOREGROUND_LOCATION_PERMISSION_REQUEST_RC,
        hasPermission = { hasForegroundPermission() }) {
      onForegroundPermissionGranted()
    }
  }

  override fun hasBackgroundPermission(): Boolean {
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      return checkPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    } else {
      return true
    }
  }

  override fun requestBackgroundPermission(fragment: Fragment) {
    if (VERSION.SDK_INT >= VERSION_CODES.Q) {
      requestPermission(
          fragment,
          BACKGROUND_LOCATION_PERMISSION_REQUEST_RC,
          android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
      )
    }
  }

  override fun onBackgroundResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    onBackgroundPermissionGranted: () -> Unit
  ) {
    onPermissionResult(
        requestCode, BACKGROUND_LOCATION_PERMISSION_REQUEST_RC,
        hasPermission = { hasBackgroundPermission() }) {
      onBackgroundPermissionGranted()
    }
  }

  override fun hasStoragePermission(): Boolean {
    return checkPermission(
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
  }

  override fun requestStoragePermission(fragment: Fragment) {
    requestPermission(
        fragment,
        BACKGROUND_LOCATION_PERMISSION_REQUEST_RC,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )
  }

  override fun onStorageResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray,
    onStoragePermissionGranted: () -> Unit
  ) {
    onPermissionResult(
        requestCode, STORAGE_PERMISSION_REQUEST_RC,
        hasPermission = { hasStoragePermission() }) {
      onStoragePermissionGranted()
    }
  }

  @CheckResult
  private fun createGeofence(fence: Fence): Geofence {
    return Geofence.Builder()
        .setRequestId(fence.id)
        .setCircularRegion(fence.lat, fence.lon, RADIUS_IN_METERS)
        .setExpirationDuration(Locator.RESCHEDULE_TIME)
        .setNotificationResponsiveness(NOTIFICATION_DELAY_IN_MILLIS)
        .setLoiteringDelay(LOITER_IN_MILLIS)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
        .build()
  }

  override fun registerGeofences(fences: List<Fence>) {
    enforcer.assertNotOnMainThread()

    if (!hasForegroundPermission()) {
      val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
      Timber.w("Cannot register Geofences, missing $permission")
      return
    }

    if (!hasBackgroundPermission()) {
      if (VERSION.SDK_INT >= VERSION_CODES.Q) {
        val permission = android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        Timber.w("Cannot register Geofences, missing $permission")
      }
      return
    }

    addGeofences(fences.map { createGeofence(it) })
  }

  @SuppressLint("MissingPermission")
  private fun addGeofences(fences: List<Geofence>) {
    // If fences is empty this will throw
    val request = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
        .addGeofences(fences)
        .build()

    removeFences {
      client.addGeofences(request, pendingIntent)
          .addOnSuccessListener { Timber.d("Registered Geofences!") }
          .addOnFailureListener { Timber.e(it, "Failed to register Geofences!") }
    }
  }

  override fun unregisterGeofences() {
    enforcer.assertNotOnMainThread()
    removeFences { Timber.d("Geofences manually unregistered") }
  }

  private inline fun removeFences(crossinline andThen: () -> Unit) {
    client.removeGeofences(pendingIntent)
        .addOnSuccessListener {
          Timber.d("Removed Geofences!")
          andThen()
        }
        .addOnFailureListener {
          Timber.e(GeofenceErrorMessages.getErrorString(context, it))
        }
  }

  companion object {

    private const val REQUEST_CODE = 1234
    private const val RADIUS_IN_METERS = 1600.0F
    private val LOITER_IN_MILLIS = TimeUnit.MINUTES.toMillis(2L)
        .toInt()
    private val NOTIFICATION_DELAY_IN_MILLIS = TimeUnit.MINUTES.toMillis(2L)
        .toInt()

    private const val BACKGROUND_LOCATION_PERMISSION_REQUEST_RC = 1234
    private const val FOREGROUND_LOCATION_PERMISSION_REQUEST_RC = 4321
    private const val STORAGE_PERMISSION_REQUEST_RC = 1324

  }

}
