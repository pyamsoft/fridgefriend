/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.db.room

import androidx.annotation.CheckResult
import com.pyamsoft.cachify.Cached
import com.pyamsoft.cachify.MultiCached1
import com.pyamsoft.cachify.MultiCached2
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryDb
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDb

internal class FridgeDbImpl internal constructor(
    db: RoomFridgeDb,

    // Items
    allItemsCache: Cached<List<FridgeItem>>,
    itemsByEntryCache: MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id>,
    sameNameDifferentPresenceCache: MultiCached2<FridgeItemDb.QuerySameNameDifferentPresenceKey, List<FridgeItem>, String, FridgeItem.Presence>,
    similarNamedCache: MultiCached2<FridgeItemDb.QuerySimilarNamedKey, List<FridgeItemDb.SimilarityScore>, FridgeItem.Id, String>,

    // Entries
    entryCache: Cached<List<FridgeEntry>>,

    // Stores
    storeCache: Cached<List<NearbyStore>>,

    // Zones
    zoneCache: Cached<List<NearbyZone>>,

    // Categories
    categoryCache: Cached<List<FridgeCategory>>
) : FridgeDb {

    private val itemDb by lazy {
        FridgeItemDb.wrap(
            allItemsCache,
            itemsByEntryCache,
            sameNameDifferentPresenceCache,
            similarNamedCache,
            db.roomItemInsertDao(),
            db.roomItemDeleteDao()
        )
    }

    private val entryDb by lazy {
        FridgeEntryDb.wrap(
            entryCache,
            db.roomEntryInsertDao(),
            db.roomEntryDeleteDao()
        )
    }

    private val storeDb by lazy {
        NearbyStoreDb.wrap(
            storeCache,
            db.roomStoreInsertDao(),
            db.roomStoreDeleteDao()
        )
    }

    private val zoneDb by lazy {
        NearbyZoneDb.wrap(
            zoneCache,
            db.roomZoneInsertDao(),
            db.roomZoneDeleteDao()
        )
    }

    private val categoryDb by lazy {
        FridgeCategoryDb.wrap(
            categoryCache,
            db.roomCategoryInsertDao(),
            db.roomCategoryDeleteDao()
        )
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
