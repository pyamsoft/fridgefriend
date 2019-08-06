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
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Delete
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Insert
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Update
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class ZoneInfoInteractor @Inject internal constructor(
  private val realtime: NearbyZoneRealtime,
  private val queryDao: NearbyZoneQueryDao
) {

  @CheckResult
  suspend fun isNearbyZoneCached(id: Long): Boolean = withContext(context = Dispatchers.Default) {
    return@withContext queryDao.query(false)
        .any { it.id() == id }
  }

  suspend inline fun listenForNearbyCacheChanges(
    id: Long,
    crossinline onInsert: (zone: NearbyZone) -> Unit,
    crossinline onDelete: (zone: NearbyZone) -> Unit
  ) = withContext(context = Dispatchers.Default) {
    realtime.listenForChanges()
        .onEvent { event ->
          return@onEvent when (event) {
            is Insert -> {
              if (event.zone.id() == id) {
                onInsert(event.zone)
              } else {
                // Ignore event for other zone
              }
            }
            is Delete -> {
              if (event.zone.id() == id) {
                onDelete(event.zone)
              } else {
                // Ignore event for other zone
              }
            }
            is Update -> {
              // Ignore Update events
              Unit
            }
          }
        }
  }
}
