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

package com.pyamsoft.fridge.db.room.dao

import androidx.annotation.CheckResult
import androidx.room.Dao
import androidx.room.Query
import com.pyamsoft.fridge.db.FridgeDbQueryDao
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.FridgeItem.Presence
import com.pyamsoft.fridge.db.room.impl.RoomFridgeItem
import io.reactivex.Maybe
import io.reactivex.Single

@Dao
internal abstract class RoomQueryDao internal constructor() : FridgeDbQueryDao {

  override fun queryAll(): Single<List<FridgeItem>> {
    return daoQueryAll()
      .toSingle(emptyList())
      .map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME}")
  @CheckResult
  internal abstract fun daoQueryAll(): Maybe<List<RoomFridgeItem>>

  override fun queryWithId(id: String): Single<FridgeItem> {
    return daoQueryWithId(id).map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE ${RoomFridgeItem.COLUMN_ID} = :id")
  @CheckResult
  internal abstract fun daoQueryWithId(id: String): Single<RoomFridgeItem>

  override fun queryWithEntryId(entryId: String): Single<List<FridgeItem>> {
    return daoQueryWithEntryId(entryId).map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE ${RoomFridgeItem.COLUMN_ENTRY_ID} = :entryId")
  @CheckResult
  internal abstract fun daoQueryWithEntryId(entryId: String): Single<List<RoomFridgeItem>>

  override fun queryWithName(name: String): Single<List<FridgeItem>> {
    return daoQueryWithName(name)
      .toSingle(emptyList())
      .map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE ${RoomFridgeItem.COLUMN_NAME} = :name")
  @CheckResult
  internal abstract fun daoQueryWithName(name: String): Maybe<List<RoomFridgeItem>>

  override fun queryWithPresence(presence: Presence): Single<List<FridgeItem>> {
    return daoQueryWithPresence(presence)
      .toSingle(emptyList())
      .map { it }
  }

  @Query("SELECT * FROM ${RoomFridgeItem.TABLE_NAME} WHERE ${RoomFridgeItem.COLUMN_PRESENCE} = :presence")
  @CheckResult
  internal abstract fun daoQueryWithPresence(presence: Presence): Maybe<List<RoomFridgeItem>>

}