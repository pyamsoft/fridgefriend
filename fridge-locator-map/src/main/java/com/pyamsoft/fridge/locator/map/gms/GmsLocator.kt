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
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.MapPermission
import kotlinx.coroutines.suspendCancellableCoroutine
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

    private val locationClient = FusedLocationProviderClient(context)
    private val settingsClient = LocationServices.getSettingsClient(context)

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

    companion object {

        private val EMPTY_CALLBACK = {}
    }
}
