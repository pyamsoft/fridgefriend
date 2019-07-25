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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import timber.log.Timber

@Dao
internal abstract class RoomFridgeItemInsertDao internal constructor() : FridgeItemInsertDao {

  override suspend fun insert(o: FridgeItem) {
    Timber.d("ROOM: Item Insert: $o")
    val roomItem = RoomFridgeItem.create(o)
    daoInsert(roomItem)
  }

  @Insert(onConflict = OnConflictStrategy.ABORT)
  internal abstract fun daoInsert(item: RoomFridgeItem)

}
