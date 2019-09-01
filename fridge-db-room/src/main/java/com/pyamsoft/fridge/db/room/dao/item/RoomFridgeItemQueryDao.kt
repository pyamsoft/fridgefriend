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

package com.pyamsoft.fridge.db.room.dao.item

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import timber.log.Timber

@Dao
internal abstract class RoomFridgeItemQueryDao internal constructor() : FridgeItemQueryDao {

    override suspend fun query(
        force: Boolean,
        entryId: String
    ): List<FridgeItem> {
        Timber.d("ROOM: Item Query: $force $entryId")
        return daoQuery(entryId)
    }

    @Query(
        "SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE ${RoomFridgeItem.COLUMN_ENTRY_ID} = :entryId"
    )
    @CheckResult
    internal abstract suspend fun daoQuery(entryId: String): List<RoomFridgeItem>

    override suspend fun query(force: Boolean): List<FridgeItem> {
        Timber.d("ROOM: Item Query: $force")
        return daoQuery()
    }

    @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME}")
    @CheckResult
    internal abstract suspend fun daoQuery(): List<RoomFridgeItem>
}
