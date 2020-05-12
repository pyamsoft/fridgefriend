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
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent
import com.pyamsoft.fridge.db.category.FridgeCategoryDb
import com.pyamsoft.fridge.db.category.FridgeCategoryDeleteDao
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.category.FridgeCategoryRealtime
import com.pyamsoft.fridge.db.category.FridgeCategoryUpdateDao
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
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class RoomFridgeDbImpl internal constructor(
    db: RoomFridgeDb,
    enforcer: Enforcer,
    entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
    itemCache: Cached1<Sequence<FridgeItem>, Boolean>,
    storeCache: Cached1<Sequence<NearbyStore>, Boolean>,
    zoneCache: Cached1<Sequence<NearbyZone>, Boolean>,
    categoryCache: Cached1<Sequence<FridgeCategory>, Boolean>
) : FridgeDb {

    private val entryRealtimeChangeBus = EventBus.create<FridgeEntryChangeEvent>()
    private val itemRealtimeChangeBus = EventBus.create<FridgeItemChangeEvent>()
    private val storeRealtimeChangeBus = EventBus.create<NearbyStoreChangeEvent>()
    private val zoneRealtimeChangeBus = EventBus.create<NearbyZoneChangeEvent>()
    private val categoryRealtimeChangeBus = EventBus.create<FridgeCategoryChangeEvent>()

    private val itemDb by lazy {
        FridgeItemDb.wrap(enforcer, object : FridgeItemDb {

            private val realtime = object : FridgeItemRealtime {

                override suspend fun listenForChanges(onChange: suspend (event: FridgeItemChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) { itemRealtimeChangeBus.onEvent(onChange) }

                override suspend fun listenForChanges(
                    id: FridgeEntry.Id,
                    onChange: suspend (event: FridgeItemChangeEvent) -> Unit
                ) = withContext(context = Dispatchers.IO) {
                    itemRealtimeChangeBus.onEvent { event ->
                        if (event.entryId == id) {
                            onChange(event)
                        }
                    }
                }
            }

            override suspend fun publish(event: FridgeItemChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    itemRealtimeChangeBus.send(event)
                }

            override fun invalidate() {
                itemCache.clear()
            }

            override fun realtime(): FridgeItemRealtime {
                return realtime
            }

            override fun queryDao(): FridgeItemQueryDao {
                return db.roomItemQueryDao()
            }

            override fun insertDao(): FridgeItemInsertDao {
                return db.roomItemInsertDao()
            }

            override fun updateDao(): FridgeItemUpdateDao {
                return db.roomItemUpdateDao()
            }

            override fun deleteDao(): FridgeItemDeleteDao {
                return db.roomItemDeleteDao()
            }
        }, itemCache)
    }

    private val entryDb by lazy {
        FridgeEntryDb.wrap(enforcer, object : FridgeEntryDb {

            private val realtime = object : FridgeEntryRealtime {
                override suspend fun listenForChanges(onChange: suspend (event: FridgeEntryChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) { entryRealtimeChangeBus.onEvent(onChange) }
            }

            override suspend fun publish(event: FridgeEntryChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    entryRealtimeChangeBus.send(event)
                }

            override fun invalidate() {
                entryCache.clear()
            }

            override fun realtime(): FridgeEntryRealtime {
                return realtime
            }

            override fun queryDao(): FridgeEntryQueryDao {
                return db.roomEntryQueryDao()
            }

            override fun insertDao(): FridgeEntryInsertDao {
                return db.roomEntryInsertDao()
            }

            override fun updateDao(): FridgeEntryUpdateDao {
                return db.roomEntryUpdateDao()
            }

            override fun deleteDao(): FridgeEntryDeleteDao {
                return db.roomEntryDeleteDao()
            }
        }, entryCache)
    }

    private val storeDb by lazy {
        NearbyStoreDb.wrap(enforcer, object : NearbyStoreDb {

            private val realtime = object : NearbyStoreRealtime {
                override suspend fun listenForChanges(onChange: suspend (event: NearbyStoreChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) { storeRealtimeChangeBus.onEvent(onChange) }
            }

            override suspend fun publish(event: NearbyStoreChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    storeRealtimeChangeBus.send(event)
                }

            override fun realtime(): NearbyStoreRealtime {
                return realtime
            }

            override fun invalidate() {
                storeCache.clear()
            }

            override fun queryDao(): NearbyStoreQueryDao {
                return db.roomStoreQueryDao()
            }

            override fun insertDao(): NearbyStoreInsertDao {
                return db.roomStoreInsertDao()
            }

            override fun updateDao(): NearbyStoreUpdateDao {
                return db.roomStoreUpdateDao()
            }

            override fun deleteDao(): NearbyStoreDeleteDao {
                return db.roomStoreDeleteDao()
            }
        }, storeCache)
    }

    private val zoneDb by lazy {
        NearbyZoneDb.wrap(enforcer, object : NearbyZoneDb {

            private val realtime = object : NearbyZoneRealtime {
                override suspend fun listenForChanges(onChange: suspend (event: NearbyZoneChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) { zoneRealtimeChangeBus.onEvent(onChange) }
            }

            override suspend fun publish(event: NearbyZoneChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    zoneRealtimeChangeBus.send(event)
                }

            override fun realtime(): NearbyZoneRealtime {
                return realtime
            }

            override fun invalidate() {
                zoneCache.clear()
            }

            override fun queryDao(): NearbyZoneQueryDao {
                return db.roomZoneQueryDao()
            }

            override fun insertDao(): NearbyZoneInsertDao {
                return db.roomZoneInsertDao()
            }

            override fun updateDao(): NearbyZoneUpdateDao {
                return db.roomZoneUpdateDao()
            }

            override fun deleteDao(): NearbyZoneDeleteDao {
                return db.roomZoneDeleteDao()
            }
        }, zoneCache)
    }

    private val categoryDb by lazy {
        FridgeCategoryDb.wrap(enforcer, object : FridgeCategoryDb {

            private val realtime = object : FridgeCategoryRealtime {
                override suspend fun listenForChanges(onChange: suspend (event: FridgeCategoryChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) {
                        categoryRealtimeChangeBus.onEvent(onChange)
                    }
            }

            override suspend fun publish(event: FridgeCategoryChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    categoryRealtimeChangeBus.send(event)
                }

            override fun invalidate() {
                categoryCache.clear()
            }

            override fun realtime(): FridgeCategoryRealtime {
                return realtime
            }

            override fun queryDao(): FridgeCategoryQueryDao {
                return db.roomCategoryQueryDao()
            }

            override fun insertDao(): FridgeCategoryInsertDao {
                return db.roomCategoryInsertDao()
            }

            override fun updateDao(): FridgeCategoryUpdateDao {
                return db.roomCategoryUpdateDao()
            }

            override fun deleteDao(): FridgeCategoryDeleteDao {
                return db.roomCategoryDeleteDao()
            }
        }, categoryCache)
    }

    @CheckResult
    override fun items(): FridgeItemDb {
        return itemDb
    }

    @CheckResult
    override fun entries(): FridgeEntryDb {
        return entryDb
    }

    @CheckResult
    override fun stores(): NearbyStoreDb {
        return storeDb
    }

    @CheckResult
    override fun zones(): NearbyZoneDb {
        return zoneDb
    }

    @CheckResult
    override fun categories(): FridgeCategoryDb {
        return categoryDb
    }
}
