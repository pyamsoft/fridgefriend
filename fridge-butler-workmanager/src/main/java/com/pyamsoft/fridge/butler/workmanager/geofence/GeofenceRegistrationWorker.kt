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

package com.pyamsoft.fridge.butler.workmanager.geofence

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.NearbyWorker
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.Locator.Fence
import com.pyamsoft.pydroid.ui.Injector
import java.util.concurrent.TimeUnit.HOURS
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class GeofenceRegistrationWorker internal constructor(
    context: Context,
    params: WorkerParameters
) : NearbyWorker(context, params) {

    private var locator: Locator? = null

    override fun onAfterInject() {
        locator = Injector.obtain(applicationContext)
    }

    override fun onAfterTeardown() {
        locator = null
    }

    override fun reschedule(butler: Butler) {
        butler.registerGeofences(Locator.RESCHEDULE_TIME, HOURS)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) = coroutineScope {
        Timber.d("GeofenceRegistrationWorker registering fences")
        return@coroutineScope withNearbyData { stores, zones ->

            val nearbyStores = stores.map { Fence.fromStore(it) }
            val nearbyZones = zones.map { Fence.fromZone(it) }
                .flatten()

            val fences = nearbyStores + nearbyZones
            requireNotNull(locator).registerGeofences(fences)
        }
    }
}
