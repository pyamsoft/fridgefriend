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

package com.pyamsoft.fridge.locator

import android.content.Context
import android.content.Intent
import android.location.Location
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.locator.map.gms.GmsGeofenceBroadcastReceiver
import com.pyamsoft.pydroid.ui.Injector
import timber.log.Timber

internal class GeofenceUpdateReceiver internal constructor() : GmsGeofenceBroadcastReceiver() {

    private var geofencer: Geofencer? = null
    private var butler: Butler? = null

    override fun onInject(context: Context) {
        geofencer = Injector.obtain(context.applicationContext)
        butler = Injector.obtain(context.applicationContext)
    }

    override fun onTeardown() {
        geofencer = null
        butler = null
    }

    override fun onGeofenceEvent(intent: Intent) {
        // Pull these out here or they will be nulled out by onTeardown
        val fencer = requireNotNull(geofencer)
        val butler = requireNotNull(butler)

        val triggeredIds = fencer.getTriggeredFenceIds(intent)
        if (triggeredIds.isEmpty()) {
            Timber.w("Geofence event received, but triggered ids were empty")
            return
        }

        // Wait and do
        fencer.onLastKnownLocationRetrieved(
            onRetrieve = { lastLocation ->
                if (lastLocation == null) {
                    Timber.w("Last Known location was null, cannot continue")
                    return@onLastKnownLocationRetrieved
                }

                processFences(butler, lastLocation, triggeredIds)
            },
            onError = { throwable ->
                Timber.e(throwable, "Error fetching last known location for nearby geofence event")
            })
    }

    private fun processFences(
        butler: Butler,
        lastLocation: Location,
        triggeredIds: List<String>
    ) {
        butler.processGeofences(lastLocation.latitude, lastLocation.longitude, triggeredIds)
    }
}
