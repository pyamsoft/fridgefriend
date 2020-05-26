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

package com.pyamsoft.fridge.db.room.entity

import androidx.annotation.CheckResult
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pyamsoft.fridge.db.entry.FridgeEntry
import java.util.Date

@Entity(tableName = RoomFridgeEntry.TABLE_NAME)
internal data class RoomFridgeEntry internal constructor(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: FridgeEntry.Id,

    @JvmField
    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @JvmField
    @ColumnInfo(name = COLUMN_CREATED_TIME)
    val createdTime: Date
) : FridgeEntry {

    @Ignore
    override fun id(): FridgeEntry.Id {
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
    override fun isReal(): Boolean {
        return true
    }

    @Ignore
    override fun name(name: String): FridgeEntry {
        return FridgeEntry.create(this, name = name, isReal = isReal())
    }

    @Ignore
    override fun makeReal(): FridgeEntry {
        return FridgeEntry.create(this, isReal = true)
    }

    companion object {

        @Ignore
        internal const val TABLE_NAME = "room_fridge_entry_table"

        @Ignore
        internal const val COLUMN_ID = "_id"

        @Ignore
        internal const val COLUMN_NAME = "name"

        @Ignore
        internal const val COLUMN_CREATED_TIME = "created_time"

        @Ignore
        @JvmStatic
        @CheckResult
        internal fun create(entry: FridgeEntry): RoomFridgeEntry {
            return if (entry is RoomFridgeEntry) entry else {
                RoomFridgeEntry(
                    entry.id(),
                    entry.name(),
                    entry.createdTime()
                )
            }
        }
    }
}
