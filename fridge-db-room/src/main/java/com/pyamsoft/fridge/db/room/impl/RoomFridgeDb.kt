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

package com.pyamsoft.fridge.db.room.impl

import androidx.annotation.CheckResult
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemDeleteDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemInsertDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemQueryDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem

@Database(entities = [RoomFridgeItem::class, RoomFridgeEntry::class], version = 1)
@TypeConverters(PresenceTypeConverter::class, DateTypeConverter::class)
internal abstract class RoomFridgeDb internal constructor() : RoomDatabase(),
  FridgeItemDb {

  private val itemDb by lazy { RoomFridgeItemDb(this) }

  @CheckResult
  internal abstract fun roomQueryDao(): RoomFridgeItemQueryDao

  @CheckResult
  internal abstract fun roomInsertDao(): RoomFridgeItemInsertDao

  @CheckResult
  internal abstract fun roomUpdateDao(): RoomFridgeItemUpdateDao

  @CheckResult
  internal abstract fun roomDeleteDao(): RoomFridgeItemDeleteDao

  override fun realtimeItems(): FridgeItemRealtime {
    return itemDb.realtimeItems()
  }

  override fun queryItems(): FridgeItemQueryDao {
    return itemDb.queryItems()
  }

  override fun insertItems(): FridgeItemInsertDao {
    return itemDb.insertItems()
  }

  override fun updateItems(): FridgeItemUpdateDao {
    return itemDb.updateItems()
  }

  override fun deleteItems(): FridgeItemDeleteDao {
    return itemDb.deleteItems()
  }

}