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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.pydroid.ui.Injector
import timber.log.Timber
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

internal class LocationProviderChangeReceiver internal constructor() : BroadcastReceiver() {

    @JvmField
    @Inject
    internal var gps: DeviceGps? = null
    @JvmField
    @Inject
    internal var butler: Butler? = null
    @JvmField
    @Inject
    internal var locator: Locator? = null

    override fun onReceive(
        context: Context?,
        intent: Intent?
    ) {
        if (context == null || intent == null) {
            Timber.w("Cannot continue, Context or Intent is null")
            return
        }

        Injector.obtain<FridgeComponent>(context.applicationContext)
            .inject(this)

        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            Timber.d("LocationProviders have changed - update Geofences")
            requireNotNull(gps).isGpsEnabled { enabled ->
                requireNotNull(butler).apply {
                    unregisterGeofences()
                    if (enabled) {
                        registerGeofences(1, SECONDS)
                    } else {
                        requireNotNull(locator).unregisterGeofences()
                    }
                }
            }
        }
    }
}
