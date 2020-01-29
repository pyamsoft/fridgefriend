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
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.locator.map.gms.GmsGeofenceBroadcastReceiver
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

internal class GeofenceUpdateReceiver internal constructor() : GmsGeofenceBroadcastReceiver() {

    @JvmField
    @Inject
    internal var processor: GeofenceProcessor? = null

    @JvmField
    @Inject
    internal var geofencer: Geofencer? = null

    override fun onInject(context: Context) {
        Injector.obtain<FridgeComponent>(context.applicationContext).inject(this)
    }

    override fun onTeardown() {
        geofencer = null
        processor = null
    }

    override suspend fun onGeofenceEvent(intent: Intent) {
        val fencer = requireNotNull(geofencer)
        val triggeredIds = fencer.getTriggeredFenceIds(intent)
        if (triggeredIds.isEmpty()) {
            Timber.w("Geofence event received, but triggered ids were empty")
            return
        }

        try {
            val lastLocation = try {
                fencer.getLastKnownLocation()
            } catch (e: Exception) {
                Timber.w("Could not get last known location - permission_button issue perhaps?")
                null
            }

            if (lastLocation == null) {
                Timber.w("Last Known location was null, cannot continue")
                return
            }

            processFences(requireNotNull(processor), lastLocation, triggeredIds)
        } catch (throwable: Throwable) {
            if (throwable !is CancellationException) {
                Timber.e(throwable, "Error fetching last known location for nearby geofence event")
            }
        }
    }

    private fun processFences(
        processor: GeofenceProcessor,
        lastLocation: Location,
        triggeredIds: List<String>
    ) {
        processor.processGeofences(lastLocation.latitude, lastLocation.longitude, triggeredIds)
    }
}
