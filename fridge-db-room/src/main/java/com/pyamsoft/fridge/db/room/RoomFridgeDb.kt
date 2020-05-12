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
 *
 */

package com.pyamsoft.fridge.db.room

import androidx.annotation.CheckResult
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
internal abstract class RoomFridgeDb internal constructor() : RoomDatabase() {

    @CheckResult
    internal abstract fun roomItemQueryDao(): RoomFridgeItemQueryDao

    @CheckResult
    internal abstract fun roomItemInsertDao(): RoomFridgeItemInsertDao

    @CheckResult
    internal abstract fun roomItemUpdateDao(): RoomFridgeItemUpdateDao

    @CheckResult
    internal abstract fun roomItemDeleteDao(): RoomFridgeItemDeleteDao

    @CheckResult
    internal abstract fun roomEntryQueryDao(): RoomFridgeEntryQueryDao

    @CheckResult
    internal abstract fun roomEntryInsertDao(): RoomFridgeEntryInsertDao

    @CheckResult
    internal abstract fun roomEntryUpdateDao(): RoomFridgeEntryUpdateDao

    @CheckResult
    internal abstract fun roomEntryDeleteDao(): RoomFridgeEntryDeleteDao

    @CheckResult
    internal abstract fun roomStoreQueryDao(): RoomNearbyStoreQueryDao

    @CheckResult
    internal abstract fun roomStoreInsertDao(): RoomNearbyStoreInsertDao

    @CheckResult
    internal abstract fun roomStoreUpdateDao(): RoomNearbyStoreUpdateDao

    @CheckResult
    internal abstract fun roomStoreDeleteDao(): RoomNearbyStoreDeleteDao

    @CheckResult
    internal abstract fun roomZoneQueryDao(): RoomNearbyZoneQueryDao

    @CheckResult
    internal abstract fun roomZoneInsertDao(): RoomNearbyZoneInsertDao

    @CheckResult
    internal abstract fun roomZoneUpdateDao(): RoomNearbyZoneUpdateDao

    @CheckResult
    internal abstract fun roomZoneDeleteDao(): RoomNearbyZoneDeleteDao

    @CheckResult
    internal abstract fun roomCategoryQueryDao(): RoomFridgeCategoryQueryDao

    @CheckResult
    internal abstract fun roomCategoryInsertDao(): RoomFridgeCategoryInsertDao

    @CheckResult
    internal abstract fun roomCategoryUpdateDao(): RoomFridgeCategoryUpdateDao

    @CheckResult
    internal abstract fun roomCategoryDeleteDao(): RoomFridgeCategoryDeleteDao
}
