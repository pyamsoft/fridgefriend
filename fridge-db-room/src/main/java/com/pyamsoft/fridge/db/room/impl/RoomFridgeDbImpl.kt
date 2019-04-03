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
import com.popinnow.android.repo.Repo
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryDeleteDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryInsertDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryQueryDao
import com.pyamsoft.fridge.db.room.dao.entry.RoomFridgeEntryUpdateDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemDeleteDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemInsertDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemQueryDao
import com.pyamsoft.fridge.db.room.dao.item.RoomFridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import com.pyamsoft.pydroid.core.threads.Enforcer

@Database(entities = [RoomFridgeItem::class, RoomFridgeEntry::class], version = 1)
@TypeConverters(PresenceTypeConverter::class, DateTypeConverter::class)
internal abstract class RoomFridgeDbImpl internal constructor() : RoomDatabase(), RoomFridgeDb {

  private val itemDb by lazy { RoomFridgeItemDb(this, enforcer, itemRepo) }
  private val entryDb by lazy {
    RoomFridgeEntryDb(this, entryRepo, object : ClearCache {

      override fun clear() {
        entryRepo.clear()
        itemRepo.clear()
      }

    })
  }

  private lateinit var enforcer: Enforcer
  private lateinit var entryRepo: Repo<List<JsonMappableFridgeEntry>>
  private lateinit var itemRepo: Repo<List<JsonMappableFridgeItem>>

  internal fun setObjects(
    enforcer: Enforcer,
    entryRepo: Repo<List<JsonMappableFridgeEntry>>,
    itemRepo: Repo<List<JsonMappableFridgeItem>>
  ) {
    this.entryRepo = entryRepo
    this.itemRepo = itemRepo
  }

  @CheckResult
  internal abstract fun roomItemQueryDao(): RoomFridgeItemQueryDao

  @CheckResult
  internal abstract fun roomItemInsertDao(): RoomFridgeItemInsertDao

  @CheckResult
  internal abstract fun roomItemUpdateDao(): RoomFridgeItemUpdateDao

  @CheckResult
  internal abstract fun roomItemDeleteDao(): RoomFridgeItemDeleteDao

  @CheckResult
  override fun items(): FridgeItemDb {
    return itemDb
  }

  @CheckResult
  internal abstract fun roomEntryQueryDao(): RoomFridgeEntryQueryDao

  @CheckResult
  internal abstract fun roomEntryInsertDao(): RoomFridgeEntryInsertDao

  @CheckResult
  internal abstract fun roomEntryUpdateDao(): RoomFridgeEntryUpdateDao

  @CheckResult
  internal abstract fun roomEntryDeleteDao(): RoomFridgeEntryDeleteDao

  @CheckResult
  override fun entries(): FridgeEntryDb {
    return entryDb
  }

}