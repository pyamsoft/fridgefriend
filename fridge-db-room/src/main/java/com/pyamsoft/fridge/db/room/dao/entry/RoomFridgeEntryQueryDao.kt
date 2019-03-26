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

package com.pyamsoft.fridge.db.room.dao.entry

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import io.reactivex.Maybe
import io.reactivex.Single
import timber.log.Timber

@Dao
internal abstract class RoomFridgeEntryQueryDao internal constructor() : FridgeEntryQueryDao {

  override fun queryAll(force: Boolean): Single<List<FridgeEntry>> {
    Timber.i("QUERY from ROOM")
    return daoQueryAll()
      .toSingle(emptyList())
      .map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeEntry.TABLE_NAME}")
  @CheckResult
  internal abstract fun daoQueryAll(): Maybe<List<RoomFridgeEntry>>

}