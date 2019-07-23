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
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.pyamsoft.fridge.locator.Geofencer
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GmsGeofencer @Inject internal constructor(private val context: Context) : Geofencer {

  override fun getTriggeredFenceIds(intent: Intent): List<String> {
    val event = GeofencingEvent.fromIntent(intent)
    if (event.hasError()) {
      val errorMessage = GeofenceErrorMessages.getErrorString(context, event.errorCode)
      Timber.e(errorMessage)
      return emptyList()
    }

    val transition = event.geofenceTransition
    if (transition != Geofence.GEOFENCE_TRANSITION_DWELL) {
      val isEnterTransition = transition == Geofence.GEOFENCE_TRANSITION_ENTER
      val transitionName = "GEOFENCE_TRANSITION_${if (isEnterTransition) "ENTER" else "EXIT"}"
      Timber.w("Ignoring Geofence, unhandled transition: $transitionName")
      return emptyList()
    }

    return event.triggeringGeofences.map { it.requestId }
  }

}
