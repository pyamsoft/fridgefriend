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

package com.pyamsoft.fridge.db.room.entity

import androidx.annotation.CheckResult
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pyamsoft.fridge.db.category.FridgeCategory
import java.util.Date

@Entity(tableName = RoomFridgeCategory.TABLE_NAME)
internal data class RoomFridgeCategory internal constructor(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: FridgeCategory.Id,

    @JvmField
    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @JvmField
    @ColumnInfo(name = COLUMN_CREATED_TIME)
    val createdTime: Date,

    @JvmField
    @ColumnInfo(name = COLUMN_DEFAULT)
    val isDefaultCategory: Boolean,

    @JvmField
    @ColumnInfo(name = COLUMN_THUMBNAIL, typeAffinity = ColumnInfo.BLOB)
    val thumbnail: FridgeCategory.Thumbnail?
) : FridgeCategory {

    @Ignore
    override fun id(): FridgeCategory.Id {
        return id
    }

    @Ignore
    override fun name(): String {
        return name
    }

    @Ignore
    override fun createdTime(): Date {
        return createdTime
    }

    @Ignore
    override fun thumbnail(): FridgeCategory.Thumbnail? {
        return thumbnail
    }

    @Ignore
    override fun isDefault(): Boolean {
        return isDefaultCategory
    }

    @Ignore
    override fun isEmpty(): Boolean {
        return id().isEmpty()
    }

    @Ignore
    override fun name(name: String): FridgeCategory {
        return FridgeCategory.create(this, name = name.trim())
    }

    @Ignore
    override fun thumbnail(thumbnail: FridgeCategory.Thumbnail): FridgeCategory {
        return FridgeCategory.create(this, thumbnail = thumbnail)
    }

    @Ignore
    override fun invalidateThumbnail(): FridgeCategory {
        return FridgeCategory.create(this, thumbnail = null)
    }

    companion object {

        @Ignore
        internal const val TABLE_NAME = "room_fridge_category_table"

        @Ignore
        internal const val COLUMN_ID = "_id"

        @Ignore
        internal const val COLUMN_NAME = "name"

        @Ignore
        internal const val COLUMN_CREATED_TIME = "created_time"

        @Ignore
        internal const val COLUMN_DEFAULT = "default"

        @Ignore
        internal const val COLUMN_THUMBNAIL = "thumbnail"

        @Ignore
        @JvmStatic
        @CheckResult
        internal fun create(entry: FridgeCategory): RoomFridgeCategory {
            return if (entry is RoomFridgeCategory) entry else {
                RoomFridgeCategory(
                    entry.id(),
                    entry.name(),
                    entry.createdTime(),
                    entry.isDefault(),
                    entry.thumbnail()
                )
            }
        }
    }
}
