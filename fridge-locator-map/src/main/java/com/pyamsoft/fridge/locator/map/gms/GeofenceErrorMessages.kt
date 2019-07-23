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

import android.content.Context
import android.content.res.Resources
import androidx.annotation.CheckResult
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.GeofenceStatusCodes
import com.pyamsoft.fridge.locator.map.R.string

internal object GeofenceErrorMessages {

  @CheckResult
  fun getErrorString(
    context: Context,
    e: Exception
  ): String {
    val appContext = context.applicationContext
    val resources = appContext.resources
    if (e is ApiException) {
      return getErrorString(resources, e.statusCode)
    } else {
      return resources.getString(string.geofence_unknown_error)
    }
  }

  @CheckResult
  fun getErrorString(
    context: Context,
    errorCode: Int
  ): String {
    return getErrorString(context.applicationContext.resources, errorCode)
  }

  @CheckResult
  private fun getErrorString(
    resources: Resources,
    errorCode: Int
  ): String {
    return when (errorCode) {
      GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
          string.geofence_not_available
      )
      GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
          string.geofence_too_many_geofences
      )
      GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
          string.geofence_too_many_pending_intents
      )
      else -> resources.getString(
          string.geofence_unknown_error
      )
    }
  }
}
