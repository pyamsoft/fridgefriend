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
import com.pyamsoft.fridge.db.room.converter.CategoryIdConverter
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.EntryIdConverter
import com.pyamsoft.fridge.db.room.converter.ItemIdConverter
import com.pyamsoft.fridge.db.room.converter.NearbyZonePointListConverter
import com.pyamsoft.fridge.db.room.converter.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.converter.StoreIdConverter
import com.pyamsoft.fridge.db.room.converter.ThumbnailTypeConverter
import com.pyamsoft.fridge.db.room.converter.ZoneIdConverter
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryDeleteDao
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryInsertDao
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryQueryDao
import com.pyamsoft.fridge.db.room.dao.category.RoomFridgeCategoryUpdateDao
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
import com.pyamsoft.fridge.db.room.entity.RoomFridgeCategory
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
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    version = 1,
    entities = [
        RoomFridgeItem::class,
        RoomFridgeEntry::class,
        RoomNearbyStore::class,
        RoomNearbyZone::class,
        RoomFridgeCategory::class
    ]
)
@TypeConverters(
    PresenceTypeConverter::class,
    DateTypeConverter::class,
    NearbyZonePointListConverter::class,
    ThumbnailTypeConverter::class,
    EntryIdConverter::class,
    ItemIdConverter::class,
    CategoryIdConverter::class,
    ZoneIdConverter::class,
    StoreIdConverter::class
)
internal abstract class RoomFridgeDbImpl internal constructor() : RoomDatabase(),
    FridgeDb {

    private val entryRealtimeChangeBus = EventBus.create<FridgeEntryChangeEvent>()
    private val itemRealtimeChangeBus = EventBus.create<FridgeItemChangeEvent>()
    private val storeRealtimeChangeBus = EventBus.create<NearbyStoreChangeEvent>()
    private val zoneRealtimeChangeBus = EventBus.create<NearbyZoneChangeEvent>()
    private val categoryRealtimeChangeBus = EventBus.create<FridgeCategoryChangeEvent>()

    private var enforcer: Enforcer? = null

    private var entryCache: Cached1<Sequence<FridgeEntry>, Boolean>? = null
    private var itemCache: Cached1<Sequence<FridgeItem>, Boolean>? = null
    private var storeCache: Cached1<Sequence<NearbyStore>, Boolean>? = null
    private var zoneCache: Cached1<Sequence<NearbyZone>, Boolean>? = null
    private var categoryCache: Cached1<Sequence<FridgeCategory>, Boolean>? = null

    private val itemDb by lazy {
        FridgeItemDb.wrap(requireNotNull(enforcer), object : FridgeItemDb {

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
                requireNotNull(itemCache).clear()
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

    private val entryDb by lazy {
        FridgeEntryDb.wrap(requireNotNull(enforcer), object : FridgeEntryDb {

            private val realtime = object : FridgeEntryRealtime {
                override suspend fun listenForChanges(onChange: suspend (event: FridgeEntryChangeEvent) -> Unit) =
                    withContext(context = Dispatchers.IO) { entryRealtimeChangeBus.onEvent(onChange) }
            }

            override suspend fun publish(event: FridgeEntryChangeEvent) =
                withContext(context = Dispatchers.Default) {
                    entryRealtimeChangeBus.send(event)
                }

            override fun invalidate() {
                requireNotNull(entryCache).clear()
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

    private val storeDb by lazy {
        NearbyStoreDb.wrap(requireNotNull(enforcer), object : NearbyStoreDb {

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
                requireNotNull(storeCache).clear()
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

    private val zoneDb by lazy {
        NearbyZoneDb.wrap(requireNotNull(enforcer), object : NearbyZoneDb {

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
                requireNotNull(zoneCache).clear()
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

    private val categoryDb by lazy {
        FridgeCategoryDb.wrap(requireNotNull(enforcer), object : FridgeCategoryDb {

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
                requireNotNull(categoryCache).clear()
            }

            override fun realtime(): FridgeCategoryRealtime {
                return realtime
            }

            override fun queryDao(): FridgeCategoryQueryDao {
                return roomCategoryQueryDao()
            }

            override fun insertDao(): FridgeCategoryInsertDao {
                return roomCategoryInsertDao()
            }

            override fun updateDao(): FridgeCategoryUpdateDao {
                return roomCategoryUpdateDao()
            }

            override fun deleteDao(): FridgeCategoryDeleteDao {
                return roomCategoryDeleteDao()
            }
        }, requireNotNull(categoryCache))
    }

    internal fun bind(
        enforcer: Enforcer,
        entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
        itemCache: Cached1<Sequence<FridgeItem>, Boolean>,
        storeCache: Cached1<Sequence<NearbyStore>, Boolean>,
        zoneCache: Cached1<Sequence<NearbyZone>, Boolean>,
        categoryCache: Cached1<Sequence<FridgeCategory>, Boolean>
    ) {
        this.enforcer = enforcer
        this.entryCache = entryCache
        this.itemCache = itemCache
        this.storeCache = storeCache
        this.zoneCache = zoneCache
        this.categoryCache = categoryCache
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

    @CheckResult
    internal abstract fun roomStoreQueryDao(): RoomNearbyStoreQueryDao

    @CheckResult
    internal abstract fun roomStoreInsertDao(): RoomNearbyStoreInsertDao

    @CheckResult
    internal abstract fun roomStoreUpdateDao(): RoomNearbyStoreUpdateDao

    @CheckResult
    internal abstract fun roomStoreDeleteDao(): RoomNearbyStoreDeleteDao

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

    @CheckResult
    override fun zones(): NearbyZoneDb {
        return zoneDb
    }

    @CheckResult
    internal abstract fun roomCategoryQueryDao(): RoomFridgeCategoryQueryDao

    @CheckResult
    internal abstract fun roomCategoryInsertDao(): RoomFridgeCategoryInsertDao

    @CheckResult
    internal abstract fun roomCategoryUpdateDao(): RoomFridgeCategoryUpdateDao

    @CheckResult
    internal abstract fun roomCategoryDeleteDao(): RoomFridgeCategoryDeleteDao

    @CheckResult
    override fun categories(): FridgeCategoryDb {
        return categoryDb
    }
}
