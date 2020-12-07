/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.locator.map.osm.popup

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Delete
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Insert
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Update
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoInteractor
import javax.inject.Inject

internal class ZoneInfoInteractorImpl @Inject internal constructor(
    butler: Butler,
    orderFactory: OrderFactory,
    realtime: NearbyZoneRealtime,
    queryDao: NearbyZoneQueryDao,
    insertDao: NearbyZoneInsertDao,
    deleteDao: NearbyZoneDeleteDao
) : ZoneInfoInteractor, BaseInfoInteractorImpl<
        NearbyZone,
        NearbyZoneChangeEvent,
        NearbyZoneRealtime,
        NearbyZoneQueryDao,
        NearbyZoneInsertDao,
        NearbyZoneDeleteDao
        >(butler, orderFactory, realtime, queryDao, insertDao, deleteDao) {

    override fun onRealtimeChange(
        event: NearbyZoneChangeEvent,
        onInsert: (NearbyZone) -> Unit,
        onDelete: (NearbyZone) -> Unit
    ) {
        return when (event) {
            is Insert -> onInsert(event.zone)
            is Delete -> onDelete(event.zone)
            is Update -> {
                // Ignore Update events
                Unit
            }
        }
    }
}
