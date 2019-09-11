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
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import timber.log.Timber

@Dao
internal abstract class RoomFridgeItemQueryDao internal constructor() : FridgeItemQueryDao {

    final override suspend fun query(force: Boolean): List<FridgeItem> {
        Timber.d("ROOM: All Items Query: $force")
        return daoQuery()
    }

    @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME}")
    @CheckResult
    internal abstract suspend fun daoQuery(): List<RoomFridgeItem>

    final override suspend fun query(
        force: Boolean,
        entryId: String
    ): List<FridgeItem> {
        throw IllegalStateException("This method should not be called")
    }

    final override suspend fun querySameNameDifferentPresence(
        force: Boolean,
        name: String,
        presence: Presence
    ): List<FridgeItem> {
        throw IllegalStateException("This method should not be called")
    }

    final override suspend fun querySimilarNamedItems(force: Boolean, item: FridgeItem): List<FridgeItem> {
        throw IllegalStateException("This method should not be called")
    }
}
