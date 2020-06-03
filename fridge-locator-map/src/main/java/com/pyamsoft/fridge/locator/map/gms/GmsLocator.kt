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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
internal class GmsLocator @Inject internal constructor(
    private val permission: MapPermission,
    context: Context
) : DeviceGps, Geofencer {

    private val locationClient by lazy { FusedLocationProviderClient(context.applicationContext) }
    private val settingsClient by lazy { LocationServices.getSettingsClient(context.applicationContext) }

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): Location? =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            if (!isGpsEnabled()) {
                Timber.w("Cannot get last location, GPS is not enabled")
                return@withContext null
            }

            throwIfMissingPermission("Cannot get last known location")

            return@withContext suspendCancellableCoroutine<Location?> { continuation ->
                Enforcer.assertOffMainThread()
                continuation.invokeOnCancellation {
                    Timber.w("getLastKnownLocation coroutine cancelled: $it")
                }

                locationClient.lastLocation
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
                    .addOnCanceledListener { continuation.resume(null) }
            }
        }

    @SuppressLint("MissingPermission")
    override suspend fun isGpsEnabled(): Boolean = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        throwIfMissingPermission("Cannot get gps state")

        return@withContext suspendCancellableCoroutine<Boolean> { continuation ->
            Enforcer.assertOffMainThread()
            continuation.invokeOnCancellation {
                Timber.w("isGpsEnabled coroutine cancelled: $it")
            }

            locationClient.locationAvailability
                .addOnSuccessListener { continuation.resume(it.isLocationAvailable) }
                .addOnFailureListener { continuation.resumeWithException(it) }
                .addOnCanceledListener { continuation.resume(false) }
        }
    }

    override suspend fun enableGps() = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        throwIfMissingPermission("Cannot enable GPS")

        suspendCancellableCoroutine<Unit> { continuation ->
            Enforcer.assertOffMainThread()
            continuation.invokeOnCancellation {
                Timber.w("enableGps coroutine cancelled: $it")
            }

            val request = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

            val settingsRequest = LocationSettingsRequest.Builder()
                .addLocationRequest(request)
                .build()

            settingsClient.checkLocationSettings(settingsRequest)
                .addOnSuccessListener { continuation.resume(Unit) }
                .addOnFailureListener { continuation.resumeWithException(it.wrapResolvable()) }
                .addOnCanceledListener { continuation.resume(Unit) }
        }
    }

    private suspend fun throwIfMissingPermission(message: String) {
        if (!permission.hasForegroundPermission()) {
            val msg = "$message, $MISSING_PERMISSION"
            Timber.w(msg)
            throw IllegalStateException(msg)
        }
    }

    @CheckResult
    private fun Throwable.wrapResolvable(): Throwable {
        return if (this.isResolvable()) GmsResolvableError(this) else this
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
        private val MISSING_PERMISSION = IllegalStateException("Missing LOCATION permissions")
    }
}
