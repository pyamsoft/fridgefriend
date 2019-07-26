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
import com.pyamsoft.fridge.db.room.dao.store.RoomNearbyStoreDeleteDao
import com.pyamsoft.fridge.db.room.dao.store.RoomNearbyStoreInsertDao
import com.pyamsoft.fridge.db.room.dao.store.RoomNearbyStoreQueryDao
import com.pyamsoft.fridge.db.room.dao.store.RoomNearbyStoreUpdateDao
import com.pyamsoft.fridge.db.room.dao.zone.RoomNearbyZoneDeleteDao
import com.pyamsoft.fridge.db.room.dao.zone.RoomNearbyZoneInsertDao
import com.pyamsoft.fridge.db.room.dao.zone.RoomNearbyZoneQueryDao
import com.pyamsoft.fridge.db.room.dao.zone.RoomNearbyZoneUpdateDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import com.pyamsoft.fridge.db.room.entity.RoomNearbyStore
import com.pyamsoft.fridge.db.room.entity.RoomNearbyZone
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.db.store.NearbyStoreUpdateDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent
import com.pyamsoft.fridge.db.zone.NearbyZoneDb
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import com.pyamsoft.fridge.db.zone.NearbyZoneUpdateDao
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.EventConsumer

@Database(
    version = 1,
    entities = [
      RoomFridgeItem::class,
      RoomFridgeEntry::class,
      RoomNearbyStore::class,
      RoomNearbyZone::class
    ]
)
@TypeConverters(
    PresenceTypeConverter::class,
    DateTypeConverter::class,
    NearbyZonePointListConverter::class
)
internal abstract class RoomFridgeDbImpl internal constructor() : RoomDatabase(),
    FridgeDb {

  private val entryRealtimeChangeBus = EventBus.create<FridgeEntryChangeEvent>()
  private val itemRealtimeChangeBus = EventBus.create<FridgeItemChangeEvent>()
  private val storeRealtimeChangeBus = EventBus.create<NearbyStoreChangeEvent>()
  private val zoneRealtimeChangeBus = EventBus.create<NearbyZoneChangeEvent>()

  private var entryCache: Cached1<Sequence<FridgeEntry>, Boolean>? = null
  private var itemCache: Cached1<Sequence<FridgeItem>, Boolean>? = null
  private var storeCache: Cached1<Sequence<NearbyStore>, Boolean>? = null
  private var zoneCache: Cached1<Sequence<NearbyZone>, Boolean>? = null

  internal fun applyCaches(
    entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
    itemCache: Cached1<Sequence<FridgeItem>, Boolean>,
    storeCache: Cached1<Sequence<NearbyStore>, Boolean>,
    zoneCache: Cached1<Sequence<NearbyZone>, Boolean>
  ) {
    this.entryCache = entryCache
    this.itemCache = itemCache
    this.storeCache = storeCache
    this.zoneCache = zoneCache
  }

  @CheckResult
  internal abstract fun roomItemQueryDao(): RoomFridgeItemQueryDao

  @CheckResult
  internal abstract fun roomItemInsertDao(): RoomFridgeItemInsertDao

  @CheckResult
  internal abstract fun roomItemUpdateDao(): RoomFridgeItemUpdateDao

  @CheckResult
  internal abstract fun roomItemDeleteDao(): RoomFridgeItemDeleteDao

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

      override fun queryDao(): FridgeItemQueryDao {
        return roomItemQueryDao()
      }

      override fun insertDao(): FridgeItemInsertDao {
        return roomItemInsertDao()
      }

      override fun updateDao(): FridgeItemUpdateDao {
        return roomItemUpdateDao()
      }

      override fun deleteDao(): FridgeItemDeleteDao {
        return roomItemDeleteDao()
      }

    }, requireNotNull(itemCache))
  }

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

      override fun queryDao(): FridgeEntryQueryDao {
        return roomEntryQueryDao()
      }

      override fun insertDao(): FridgeEntryInsertDao {
        return roomEntryInsertDao()
      }

      override fun updateDao(): FridgeEntryUpdateDao {
        return roomEntryUpdateDao()
      }

      override fun deleteDao(): FridgeEntryDeleteDao {
        return roomEntryDeleteDao()
      }

    }, requireNotNull(entryCache))
  }

  @CheckResult
  override fun entries(): FridgeEntryDb {
    return entryDb
  }

  @CheckResult
  internal abstract fun roomStoreQueryDao(): RoomNearbyStoreQueryDao

  @CheckResult
  internal abstract fun roomStoreInsertDao(): RoomNearbyStoreInsertDao

  @CheckResult
  internal abstract fun roomStoreUpdateDao(): RoomNearbyStoreUpdateDao

  @CheckResult
  internal abstract fun roomStoreDeleteDao(): RoomNearbyStoreDeleteDao

  private val storeDb by lazy {
    NearbyStoreDb.wrap(object : NearbyStoreDb {

      private val realtime by lazy {
        object : NearbyStoreRealtime {
          override fun listenForChanges(): EventConsumer<NearbyStoreChangeEvent> {
            return storeRealtimeChangeBus
          }
        }
      }

      override fun publish(event: NearbyStoreChangeEvent) {
        storeRealtimeChangeBus.publish(event)
      }

      override fun realtime(): NearbyStoreRealtime {
        return realtime
      }

      override fun queryDao(): NearbyStoreQueryDao {
        return roomStoreQueryDao()
      }

      override fun insertDao(): NearbyStoreInsertDao {
        return roomStoreInsertDao()
      }

      override fun updateDao(): NearbyStoreUpdateDao {
        return roomStoreUpdateDao()
      }

      override fun deleteDao(): NearbyStoreDeleteDao {
        return roomStoreDeleteDao()
      }

    }, requireNotNull(storeCache))
  }

  @CheckResult
  override fun stores(): NearbyStoreDb {
    return storeDb
  }

  @CheckResult
  internal abstract fun roomZoneQueryDao(): RoomNearbyZoneQueryDao

  @CheckResult
  internal abstract fun roomZoneInsertDao(): RoomNearbyZoneInsertDao

  @CheckResult
  internal abstract fun roomZoneUpdateDao(): RoomNearbyZoneUpdateDao

  @CheckResult
  internal abstract fun roomZoneDeleteDao(): RoomNearbyZoneDeleteDao

  private val zoneDb by lazy {
    NearbyZoneDb.wrap(object : NearbyZoneDb {

      private val realtime by lazy {
        object : NearbyZoneRealtime {
          override fun listenForChanges(): EventConsumer<NearbyZoneChangeEvent> {
            return zoneRealtimeChangeBus
          }
        }
      }

      override fun publish(event: NearbyZoneChangeEvent) {
        zoneRealtimeChangeBus.publish(event)
      }

      override fun realtime(): NearbyZoneRealtime {
        return realtime
      }

      override fun queryDao(): NearbyZoneQueryDao {
        return roomZoneQueryDao()
      }

      override fun insertDao(): NearbyZoneInsertDao {
        return roomZoneInsertDao()
      }

      override fun updateDao(): NearbyZoneUpdateDao {
        return roomZoneUpdateDao()
      }

      override fun deleteDao(): NearbyZoneDeleteDao {
        return roomZoneDeleteDao()
      }

    }, requireNotNull(zoneCache))
  }

  @CheckResult
  override fun zones(): NearbyZoneDb {
    return zoneDb
  }

}
