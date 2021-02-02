/*
 * Copyright 2021 Peter Kenji Yamanaka
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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.pyamsoft.fridge.db.room.converter.CategoryIdConverter
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.EntryIdConverter
import com.pyamsoft.fridge.db.room.converter.ItemIdConverter
import com.pyamsoft.fridge.db.room.converter.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.converter.ThumbnailTypeConverter
import com.pyamsoft.fridge.db.room.entity.RoomFridgeCategory
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem

@Database(
    version = 2,
    entities = [
        RoomFridgeItem::class,
        RoomFridgeEntry::class,
        RoomFridgeCategory::class
    ]
)
@TypeConverters(
    PresenceTypeConverter::class,
    DateTypeConverter::class,
    ThumbnailTypeConverter::class,
    EntryIdConverter::class,
    ItemIdConverter::class,
    CategoryIdConverter::class,
)
internal abstract class RoomFridgeDbImpl internal constructor() : RoomDatabase(), RoomFridgeDb
