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

import androidx.annotation.CheckResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes

internal object GeofenceErrorMessages {

    private const val UNKNOWN_ERROR = "An unknown error occurred, please try again later."

    @CheckResult
    fun getErrorString(e: Exception): String {
        return if (e is ApiException) {
            getErrorString(e.statusCode)
        } else {
            UNKNOWN_ERROR
        }
    }

    @CheckResult
    fun getErrorString(errorCode: Int): String {
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "Sorry, the Geofence service is not available now"
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "Unable to register Geofence: Geofence count exceeds per-app limit of 100."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "Unable to register Geofence: Too many request objects."
            else -> UNKNOWN_ERROR
        }
    }
}
