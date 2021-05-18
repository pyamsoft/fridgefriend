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

package com.pyamsoft.fridge.db.room.dao.entry

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomFridgeEntryQueryDao internal constructor() : FridgeEntryQueryDao {

  final override suspend fun query(force: Boolean): List<FridgeEntry> =
      withContext(context = Dispatchers.IO) { daoQuery() }

  @CheckResult
  @Query("SELECT * FROM ${RoomFridgeEntry.TABLE_NAME}")
  internal abstract suspend fun daoQuery(): List<RoomFridgeEntry>
}
