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

import android.app.Activity
import android.content.Context
import android.location.Location
import androidx.annotation.CheckResult
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
internal class GmsLocator @Inject internal constructor(
    private val permission: MapPermission,
    private val enforcer: Enforcer,
    context: Context
) : DeviceGps, Geofencer {

    private val locationClient = FusedLocationProviderClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)

    override suspend fun getLastKnownLocation(): Location? {
        enforcer.assertNotOnMainThread()
        if (!isGpsEnabled()) {
            Timber.w("Cannot get last location, GPS is not enabled")
            return null
        }

        return suspendCancellableCoroutine { continuation ->
            enforcer.assertNotOnMainThread()
            continuation.invokeOnCancellation {
                Timber.w("getLastKnownLocation coroutine cancelled: $it")
            }

            if (!permission.hasForegroundPermission()) {
                Timber.w("Cannot get last location, missing foreground location permissions")
                continuation.resumeWithException(MISSING_PERMISSION)
            } else {
                locationClient.lastLocation
                    .addOnSuccessListener { location ->
                        Timber.d("Last known location: $location")
                        continuation.resume(location)
                    }
                    .addOnFailureListener { throwable ->
                        Timber.e(throwable, "Error getting last known location")
                        continuation.resumeWithException(throwable)
                    }.addOnCanceledListener {
                        Timber.w("Last known location cancelled")
                        continuation.resume(null)
                    }
            }
        }
    }

    override suspend fun isGpsEnabled(): Boolean {
        enforcer.assertNotOnMainThread()
        return suspendCancellableCoroutine { continuation ->
            enforcer.assertNotOnMainThread()
            continuation.invokeOnCancellation {
                Timber.w("isGpsEnabled coroutine cancelled: $it")
            }

            locationClient.locationAvailability
                .addOnSuccessListener { available ->
                    val enabled = available.isLocationAvailable
                    Timber.d("Is gps enabled: $enabled")
                    continuation.resume(enabled)

                }
                .addOnFailureListener { throwable ->
                    Timber.e(throwable, "Error getting isGpsEnabled")
                    continuation.resumeWithException(throwable)
                }
                .addOnCanceledListener {
                    Timber.w("isGpsEnabled cancelled")
                    continuation.resume(false)
                }
        }
    }

    override suspend fun enableGps() {
        enforcer.assertNotOnMainThread()

        try {
            requestEnableGps()
        } catch (e: Throwable) {
            throw if (e.isResolvable()) GmsResolvableError(e) else e
        }
    }

    private suspend inline fun requestEnableGps() {
        return suspendCancellableCoroutine { continuation ->
            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            val settingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .build()

            settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it) }
                .addOnCanceledListener { continuation.resume(Unit) }

        }
    }

    @CheckResult
    private fun Throwable.isResolvable(): Boolean {
        return this is ResolvableApiException && this.statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED
    }

    private class GmsResolvableError internal constructor(
        private val error: Throwable
    ) : Exception(error.message), DeviceGps.ResolvableError {

        override fun resolve(activity: Activity) {
            try {
                Timber.d("Attempt to resolve GPS enable error")
                val resolvable = error as ResolvableApiException
                resolvable.startResolutionForResult(activity, DeviceGps.ENABLE_GPS_REQUEST_CODE)
            } catch (e: Throwable) {
                throw e
            }
        }
    }

    companion object {
        private val MISSING_PERMISSION =
            IllegalStateException("Missing ACCESS_FOREGROUND_LOCATION permission_button")
    }
}
