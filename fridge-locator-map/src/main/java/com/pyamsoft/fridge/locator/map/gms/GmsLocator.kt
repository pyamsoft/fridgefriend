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
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.annotation.CheckResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.pyamsoft.fridge.core.Core
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.GeofenceBroadcastReceiver
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.fridge.locator.MapPermission
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

@Singleton
internal class GmsLocator @Inject internal constructor(
    context: Context,
    private val permission: MapPermission,
    geofenceReceiverClass: Class<out GeofenceBroadcastReceiver>
) : Locator, DeviceGps, Geofencer {

    private val locationClient = FusedLocationProviderClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    private val geofenceIntent = PendingIntent.getBroadcast(
        context, GEOFENCE_REQUEST_CODE, Intent(context, geofenceReceiverClass),
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    override suspend fun getLastKnownLocation(): Location? {
        return suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation {
                Timber.w("Get last known coroutine cancelled: $it")
            }

            fetchLocation(
                onRetrieve = { continuation.resume(it) },
                onError = { continuation.resumeWithException(it) },
                onCancel = { continuation.cancel() }
            )
        }
    }

    @SuppressLint("MissingPermission")
    private inline fun fetchLocation(
        crossinline onRetrieve: (lastLocation: Location?) -> Unit,
        crossinline onError: (throwable: Throwable) -> Unit,
        crossinline onCancel: () -> Unit
    ) {
        if (!permission.hasForegroundPermission()) {
            Timber.w("Cannot get last location, missing foreground location permissions")
            onError(IllegalStateException("Missing ACCESS_FOREGROUND_LOCATION permission_button"))
            return
        }

        isGpsEnabled { enabled ->
            if (!enabled) {
                Timber.w("Cannot get last location, GPS is not enabled")
                onRetrieve(null)
                return@isGpsEnabled
            }

            locationClient.lastLocation
                .addOnSuccessListener { location ->
                    Timber.d("Last known location: $location")
                    onRetrieve(location)
                }
                .addOnFailureListener { throwable ->
                    Timber.e(throwable, "Error getting last known location")
                    onError(throwable)
                }.addOnCanceledListener {
                    Timber.w("Last known location cancelled")
                    onCancel()
                }
        }
    }

    override fun isGpsEnabled(func: (enabled: Boolean) -> Unit) {
        checkGpsSettings(onEnabled = { func(true) }, onDisabled = { func(false) })
    }

    override fun enableGps(
        activity: Activity,
        onError: (throwable: Throwable) -> Unit
    ) {
        checkGpsSettings(onEnabled = EMPTY_CALLBACK, onDisabled = { throwable ->
            if (throwable !is ApiException) {
                onError(throwable)
            } else {
                return@checkGpsSettings when (throwable.statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        if (throwable !is ResolvableApiException) {
                            onError(throwable)
                        } else {
                            try {
                                throwable.startResolutionForResult(
                                    activity,
                                    DeviceGps.ENABLE_GPS_REQUEST_CODE
                                )
                            } catch (e: Exception) {
                                onError(e)
                            }
                        }
                    }
                    else -> onError(throwable)
                }
            }
        })
    }

    private inline fun checkGpsSettings(
        crossinline onEnabled: () -> Unit,
        crossinline onDisabled: (throwable: Throwable) -> Unit
    ) {
        val request = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val settingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(request)
            .build()

        settingsClient.checkLocationSettings(settingsRequest)
            .addOnSuccessListener { onEnabled() }
            .addOnFailureListener { onDisabled(it) }
    }

    override fun getTriggeredFenceIds(intent: Intent): List<String> {
        val event = GeofencingEvent.fromIntent(intent)
        if (event.hasError()) {
            val errorMessage = GeofenceErrorMessages.getErrorString(event.errorCode)
            Timber.e(errorMessage)
            return emptyList()
        }

        val transition = event.geofenceTransition
        if (transition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Timber.w("Ignoring Geofence, transition: GEOFENCE_TRANSITION_EXIT")
            return emptyList()
        }

        return event.triggeringGeofences.map { it.requestId }
    }

    @CheckResult
    private fun createGeofence(fence: Fence): Geofence {
        val triggers = Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER
        return Geofence.Builder()
            .setRequestId(fence.id)
            .setCircularRegion(fence.lat, fence.lon, Locator.RADIUS_IN_METERS)
            .setExpirationDuration(Core.RESCHEDULE_TIME)
            .setNotificationResponsiveness(NOTIFICATION_DELAY_IN_MILLIS)
            .setLoiteringDelay(LOITER_IN_MILLIS)
            .setTransitionTypes(triggers)
            .build()
    }

    override fun registerGeofences(fences: List<Fence>) {
        if (!permission.hasForegroundPermission()) {
            Timber.w("Cannot register Geofences, missing foreground location permissions")
            return
        }

        if (!permission.hasBackgroundPermission()) {
            Timber.w("Cannot register Geofences, missing background location permissions")
            return
        }

        addGeofences(fences.map { createGeofence(it) })
    }

    @SuppressLint("MissingPermission")
    private fun addGeofences(fences: List<Geofence>) {
        removeFences {
            if (fences.isEmpty()) {
                Timber.w("Cannot register empty list of geofences")
                return@removeFences
            }

            isGpsEnabled { enabled ->
                if (!enabled) {
                    Timber.w("Cannot register Geofences, GPS is not enabled")
                    return@isGpsEnabled
                }

                if (fences.size > Locator.MAX_GEOFENCE_ALLOWED_COUNT) {
                    Timber.w("Cannot register Geofences, too many: ${fences.size}")
                    return@isGpsEnabled
                }

                // If fences is empty this will throw
                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
                    .addGeofences(fences)
                    .build()

                geofencingClient.addGeofences(request, geofenceIntent)
                    .addOnSuccessListener { Timber.d("Registered Geofences!") }
                    .addOnFailureListener { Timber.e(it, "Failed to register Geofences!") }
            }
        }
    }

    override fun unregisterGeofences() {
        removeGeofences()
    }

    private fun removeGeofences() {
        removeFences { Timber.d("Geofences manually unregistered") }
    }

    private inline fun removeFences(crossinline andThen: () -> Unit) {
        geofencingClient.removeGeofences(geofenceIntent)
            .addOnSuccessListener { andThen() }
            .addOnFailureListener {
                Timber.e(GeofenceErrorMessages.getErrorString(it))
            }
    }

    companion object {

        private val EMPTY_CALLBACK = {}

        private const val GEOFENCE_REQUEST_CODE = 2563

        private val LOITER_IN_MILLIS = TimeUnit.MINUTES.toMillis(2L)
            .toInt()
        private val NOTIFICATION_DELAY_IN_MILLIS = TimeUnit.MINUTES.toMillis(2L)
            .toInt()
    }
}
