/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.db.room.dao.item

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomFridgeItemInsertDao internal constructor() : FridgeItemInsertDao {

  final override suspend fun insert(o: FridgeItem) =
      withContext(context = Dispatchers.IO) {
        val roomItem = RoomFridgeItem.create(o)
        return@withContext if (daoQuery(roomItem.id()) == null) {
          daoInsert(roomItem)
          true
        } else {
          daoUpdate(roomItem)
          false
        }
      }

  @Insert(onConflict = OnConflictStrategy.ABORT)
  internal abstract fun daoInsert(item: RoomFridgeItem)

  @CheckResult
  @Query(
      """
        SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE
        ${RoomFridgeItem.COLUMN_ID} = :id
        LIMIT 1
        """)
  internal abstract suspend fun daoQuery(id: FridgeItem.Id): RoomFridgeItem?

  @Update internal abstract suspend fun daoUpdate(entry: RoomFridgeItem)
}
