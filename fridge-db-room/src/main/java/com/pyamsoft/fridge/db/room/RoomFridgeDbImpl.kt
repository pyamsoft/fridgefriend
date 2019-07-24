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

package com.pyamsoft.fridge.db.room

import androidx.annotation.CheckResult
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.NearbyZonePointListConverter
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
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.EventConsumer

@Database(entities = [RoomFridgeItem::class, RoomFridgeEntry::class], version = 1)
@TypeConverters(
    PresenceTypeConverter::class,
    DateTypeConverter::class,
    NearbyZonePointListConverter::class
)
internal abstract class RoomFridgeDbImpl internal constructor() : RoomDatabase(),
    FridgeDb {

  private val entryRealtimeChangeBus = EventBus.create<FridgeEntryChangeEvent>()
  private val itemRealtimeChangeBus = EventBus.create<FridgeItemChangeEvent>()

  private var entryCache: Cached1<Sequence<FridgeEntry>, Boolean>? = null
  private var itemCache: Cached1<Sequence<FridgeItem>, Boolean>? = null

  internal fun setObjects(
    entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
    itemCache: Cached1<Sequence<FridgeItem>, Boolean>
  ) {
    this.entryCache = entryCache
    this.itemCache = itemCache
  }

  private val itemDb by lazy {
    FridgeItemDb.wrap(object : FridgeItemDb {

      private val realtime by lazy {
        object : FridgeItemRealtime {

          override fun listenForChanges(): EventConsumer<FridgeItemChangeEvent> {
            return itemRealtimeChangeBus
          }

          override fun listenForChanges(entryId: String): EventConsumer<FridgeItemChangeEvent> {
            return object : EventConsumer<FridgeItemChangeEvent> {

              override suspend fun onEvent(emitter: suspend (event: FridgeItemChangeEvent) -> Unit) {
                itemRealtimeChangeBus.onEvent { event ->
                  if (event.entryId == entryId) {
                    emitter(event)
                  }
                }
              }

            }
          }

        }
      }

      override fun publish(event: FridgeItemChangeEvent) {
        itemRealtimeChangeBus.publish(event)
      }

      override fun realtime(): FridgeItemRealtime {
        return realtime
      }

      override fun query(): FridgeItemQueryDao {
        return roomItemQueryDao()
      }

      override fun insert(): FridgeItemInsertDao {
        return roomItemInsertDao()
      }

      override fun update(): FridgeItemUpdateDao {
        return roomItemUpdateDao()
      }

      override fun delete(): FridgeItemDeleteDao {
        return roomItemDeleteDao()
      }

    }, requireNotNull(itemCache))
  }
  private val entryDb by lazy {
    FridgeEntryDb.wrap(object : FridgeEntryDb {

      private val realtime by lazy {
        object : FridgeEntryRealtime {
          override fun listenForChanges(): EventConsumer<FridgeEntryChangeEvent> {
            return entryRealtimeChangeBus
          }
        }
      }

      override fun publish(event: FridgeEntryChangeEvent) {
        entryRealtimeChangeBus.publish(event)
      }

      override fun realtime(): FridgeEntryRealtime {
        return realtime
      }

      override fun query(): FridgeEntryQueryDao {
        return roomEntryQueryDao()
      }

      override fun insert(): FridgeEntryInsertDao {
        return roomEntryInsertDao()
      }

      override fun update(): FridgeEntryUpdateDao {
        return roomEntryUpdateDao()
      }

      override fun delete(): FridgeEntryDeleteDao {
        return roomEntryDeleteDao()
      }

    }, requireNotNull(entryCache)) {
      entryCache?.clear()
      itemCache?.clear()
    }
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
