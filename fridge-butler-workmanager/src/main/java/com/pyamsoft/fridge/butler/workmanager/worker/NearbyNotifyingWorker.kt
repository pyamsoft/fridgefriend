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
import com.pyamsoft.fridge.butler.workmanager.geofence.GeofenceNotifications
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import timber.log.Timber

internal abstract class NearbyNotifyingWorker protected constructor(
    context: Context,
    params: WorkerParameters
) : NearbyWorker(context, params) {

    protected suspend fun fireNotifications(
        storeNotifications: Set<NearbyStore>,
        zoneNotifications: Set<NearbyZone>
    ) {
        if (storeNotifications.isEmpty() && zoneNotifications.isEmpty()) {
            Timber.w("Cannot process a completely empty event")
            return
        }

        withFridgeData { entry, items ->
            val neededItems = items.filterNot { it.isArchived() }
                .filter { it.presence() == NEED }
            if (neededItems.isEmpty()) {
                Timber.w("There are nearby's but nothing is needed")
                return@withFridgeData
            }

            storeNotifications.forEach { store ->
                notification { handler, foregroundState ->
                    GeofenceNotifications.notifyNeeded(
                        handler, foregroundState, applicationContext, entry, store, neededItems
                    )
                }
            }
            zoneNotifications.forEach { zone ->
                notification { handler, foregroundState ->
                    GeofenceNotifications.notifyNeeded(
                        handler, foregroundState, applicationContext, entry, zone, neededItems
                    )
                }
            }
        }
    }
}
