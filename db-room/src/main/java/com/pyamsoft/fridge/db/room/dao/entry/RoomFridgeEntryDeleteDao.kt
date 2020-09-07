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

package com.pyamsoft.fridge.db.room.dao.entry

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomFridgeEntryDeleteDao internal constructor() : FridgeEntryDeleteDao {

    final override suspend fun delete(o: FridgeEntry): Boolean =
        withContext(context = Dispatchers.IO) {
            val roomEntry = RoomFridgeEntry.create(o)
            return@withContext daoDelete(roomEntry) > 0
        }

    @Delete
    @CheckResult
    internal abstract fun daoDelete(entry: RoomFridgeEntry): Int

    final override suspend fun deleteAll() = withContext(context = Dispatchers.IO) {
        daoDeleteAll()
    }

    @Query("DELETE FROM ${RoomFridgeEntry.TABLE_NAME} WHERE 1 = 1")
    internal abstract fun daoDeleteAll()
}
