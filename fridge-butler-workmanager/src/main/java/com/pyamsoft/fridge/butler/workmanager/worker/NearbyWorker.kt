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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal abstract class NearbyWorker protected constructor(
    context: Context,
    params: WorkerParameters
) : FridgeWorker(context, params) {

    private var storeDb: NearbyStoreQueryDao? = null
    private var zoneDb: NearbyZoneQueryDao? = null

    final override fun afterInject() {
        storeDb = Injector.obtain(applicationContext)
        zoneDb = Injector.obtain(applicationContext)
        onAfterInject()
    }

    final override fun afterTeardown() {
        storeDb = null
        zoneDb = null
        onAfterTeardown()
    }

    protected open fun onAfterInject() {
    }

    protected open fun onAfterTeardown() {
    }

    protected suspend fun withNearbyData(func: suspend (stores: List<NearbyStore>, zones: List<NearbyZone>) -> Unit) =
        coroutineScope {
            val storeJob = async { requireNotNull(storeDb).query(false) }
            val zoneJob = async { requireNotNull(zoneDb).query(false) }
            val nearbyStores = storeJob.await()
            val nearbyZones = zoneJob.await()
            func(nearbyStores, nearbyZones)
        }
}
