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

package com.pyamsoft.fridge.db.room.dao.zone

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pyamsoft.fridge.db.room.entity.RoomNearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomNearbyZoneInsertDao internal constructor() : NearbyZoneInsertDao {

    override suspend fun insert(o: NearbyZone): Boolean = withContext(context = Dispatchers.IO) {
        val roomNearbyZone = RoomNearbyZone.create(o)
        return@withContext if (daoQuery(roomNearbyZone.id()) == null) {
            daoInsert(roomNearbyZone)
            true
        } else {
            daoUpdate(roomNearbyZone)
            false
        }
    }

    @Insert(onConflict = OnConflictStrategy.ABORT)
    internal abstract fun daoInsert(entry: RoomNearbyZone)


    @CheckResult
    @Query(
        """
        SELECT * FROM ${RoomNearbyZone.TABLE_NAME} WHERE 
        ${RoomNearbyZone.COLUMN_ID} = :id
        LIMIT 1
        """
    )
    internal abstract suspend fun daoQuery(id: NearbyZone.Id): RoomNearbyZone?

    @Update
    internal abstract suspend fun daoUpdate(zone: RoomNearbyZone)
}
