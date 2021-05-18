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

package com.pyamsoft.fridge.db.room.dao.category

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomFridgeCategoryInsertDao internal constructor() :
    FridgeCategoryInsertDao {

  final override suspend fun insert(o: FridgeCategory): Boolean =
      withContext(context = Dispatchers.IO) {
        val roomCategory = RoomFridgeCategory.create(o)
        return@withContext if (daoQuery(roomCategory.id()) == null) {
          daoInsert(roomCategory)
          true
        } else {
          daoUpdate(roomCategory)
          false
        }
      }

  @Insert(onConflict = OnConflictStrategy.ABORT)
  internal abstract fun daoInsert(entry: RoomFridgeCategory)

  @CheckResult
  @Query(
      """
        SELECT * FROM ${RoomFridgeCategory.TABLE_NAME} WHERE
        ${RoomFridgeCategory.COLUMN_ID} = :id
        LIMIT 1
        """)
  internal abstract suspend fun daoQuery(id: FridgeCategory.Id): RoomFridgeCategory?

  @Update internal abstract suspend fun daoUpdate(category: RoomFridgeCategory)
}
