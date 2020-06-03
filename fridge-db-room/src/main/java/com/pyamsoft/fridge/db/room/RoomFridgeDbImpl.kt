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
import com.pyamsoft.fridge.db.category.FridgeCategoryDb
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDb
import com.pyamsoft.pydroid.core.Enforcer

internal class RoomFridgeDbImpl internal constructor(
    db: RoomFridgeDb,
    entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
    itemCache: Cached1<Sequence<FridgeItem>, Boolean>,
    storeCache: Cached1<Sequence<NearbyStore>, Boolean>,
    zoneCache: Cached1<Sequence<NearbyZone>, Boolean>,
    categoryCache: Cached1<Sequence<FridgeCategory>, Boolean>
) : FridgeDb {

    private val itemDb by lazy {
        FridgeItemDb.wrap(
            itemCache,
            db.roomItemInsertDao(),
            db.roomItemUpdateDao(),
            db.roomItemDeleteDao()
        )
    }

    private val entryDb by lazy {
        FridgeEntryDb.wrap(
            entryCache,
            db.roomEntryInsertDao(),
            db.roomEntryUpdateDao(),
            db.roomEntryDeleteDao()
        )
    }

    private val storeDb by lazy {
        NearbyStoreDb.wrap(
            storeCache,
            db.roomStoreInsertDao(),
            db.roomStoreUpdateDao(),
            db.roomStoreDeleteDao()
        )
    }

    private val zoneDb by lazy {
        NearbyZoneDb.wrap(
            zoneCache,
            db.roomZoneInsertDao(),
            db.roomZoneUpdateDao(),
            db.roomZoneDeleteDao()
        )
    }

    private val categoryDb by lazy {
        FridgeCategoryDb.wrap(
            categoryCache,
            db.roomCategoryInsertDao(),
            db.roomCategoryUpdateDao(),
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
