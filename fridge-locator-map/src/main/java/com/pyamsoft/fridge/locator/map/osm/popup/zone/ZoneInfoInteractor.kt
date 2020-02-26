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

package com.pyamsoft.fridge.locator.map.osm.popup.zone

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Delete
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Insert
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Update
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class ZoneInfoInteractor @Inject internal constructor(
    private val butler: Butler,
    private val realtime: NearbyZoneRealtime,
    private val queryDao: NearbyZoneQueryDao,
    private val insertDao: NearbyZoneInsertDao,
    private val deleteDao: NearbyZoneDeleteDao
) {

    /**
     * TODO Move out into the parent level MapInteractor
     */
    @CheckResult
    suspend fun getAllCachedZones(): List<NearbyZone> {
        return withContext(context = Dispatchers.Default) { queryDao.query(false) }
    }

    /**
     * TODO Move out into the parent level MapInteractor
     */
    suspend inline fun listenForNearbyCacheChanges(
        crossinline onInsert: (zone: NearbyZone) -> Unit,
        crossinline onDelete: (zone: NearbyZone) -> Unit
    ) = withContext(context = Dispatchers.Default) {
        realtime.listenForChanges()
            .onEvent { event ->
                return@onEvent when (event) {
                    is Insert -> onInsert(event.zone)
                    is Delete -> onDelete(event.zone)
                    is Update -> {
                        // Ignore Update events
                        Unit
                    }
                }
            }
    }

    suspend fun deleteZoneFromDb(zone: NearbyZone) = withContext(context = Dispatchers.Default) {
        deleteDao.delete(zone)
        restartLocationWorker()
    }

    suspend fun insertZoneIntoDb(zone: NearbyZone) = withContext(context = Dispatchers.Default) {
        insertDao.insert(zone)
        restartLocationWorker()
    }

    private suspend fun restartLocationWorker() {
        butler.cancelLocationReminder()
        butler.remindLocation(
            Butler.Parameters(forceNotification = true)
        )
    }
}
