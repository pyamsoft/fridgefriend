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
import androidx.room.Query
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomFridgeItemQueryDao internal constructor() : FridgeItemQueryDao {

  final override suspend fun query(force: Boolean): List<FridgeItem> =
      withContext(context = Dispatchers.IO) { daoQuery() }

  @CheckResult
  @Query("""SELECT * FROM ${RoomFridgeItem.TABLE_NAME}""")
  internal abstract suspend fun daoQuery(): List<RoomFridgeItem>

  final override suspend fun query(force: Boolean, id: FridgeEntry.Id): List<FridgeItem> =
      withContext(context = Dispatchers.IO) { daoQuery(id) }

  @CheckResult
  @Query(
      """
        SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE
        ${RoomFridgeItem.COLUMN_ENTRY_ID} = :id
        """)
  internal abstract suspend fun daoQuery(id: FridgeEntry.Id): List<RoomFridgeItem>

  final override suspend fun querySameNameDifferentPresence(
      force: Boolean,
      name: String,
      presence: FridgeItem.Presence
  ): List<FridgeItem> =
      withContext(context = Dispatchers.IO) { daoQuerySameNameDifferentPresence(name, presence) }

  @CheckResult
  @Query(
      """
        SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE
        ${RoomFridgeItem.COLUMN_PRESENCE} != :presence AND
        ${RoomFridgeItem.COLUMN_CONSUMED} IS NULL AND
        ${RoomFridgeItem.COLUMN_SPOILED} IS NULL AND
        TRIM(${RoomFridgeItem.COLUMN_NAME})  = :name COLLATE NOCASE
        """)
  internal abstract suspend fun daoQuerySameNameDifferentPresence(
      name: String,
      presence: FridgeItem.Presence
  ): List<RoomFridgeItem>

  final override suspend fun querySimilarNamedItems(
      force: Boolean,
      id: FridgeItem.Id,
      name: String
  ): List<FridgeItem> = withContext(context = Dispatchers.IO) { daoQuerySimilarNamedItems(id) }

  // Filtering done in Kotlin via distance algorithm
  @CheckResult
  @Query(
      """
        SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE
        ${RoomFridgeItem.COLUMN_ID} != :id
        """)
  internal abstract suspend fun daoQuerySimilarNamedItems(id: FridgeItem.Id): List<RoomFridgeItem>
}
