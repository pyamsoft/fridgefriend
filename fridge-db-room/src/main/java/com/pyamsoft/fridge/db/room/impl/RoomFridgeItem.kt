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

package com.pyamsoft.fridge.db.room.impl

import androidx.annotation.CheckResult
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.FridgeItem.Presence
import java.util.Date

@Entity(tableName = RoomFridgeItem.TABLE_NAME)
internal data class RoomFridgeItem internal constructor(
  @field:[PrimaryKey ColumnInfo(name = COLUMN_ID)]
  val id: String,

  @field:ColumnInfo(name = COLUMN_NAME)
  val name: String,

  @field:ColumnInfo(name = COLUMN_EXPIRE_TIME)
  val expireTime: Date,

  @field:ColumnInfo(name = COLUMN_PRESENCE)
  val presence: Presence
) : FridgeItem {

  @Ignore
  override fun id(): String {
    return id
  }

  @Ignore
  override fun name(): String {
    return name
  }

  @Ignore
  override fun expireTime(): Date {
    return expireTime
  }

  @Ignore
  override fun presence(): Presence {
    return presence
  }

  companion object {

    internal const val TABLE_NAME = "room_fridge_item_table"

    internal const val COLUMN_ID = "_id"
    internal const val COLUMN_NAME = "name"
    internal const val COLUMN_EXPIRE_TIME = "expire_time"
    internal const val COLUMN_PRESENCE = "presence"

    @JvmStatic
    @CheckResult
    internal fun create(item: FridgeItem): RoomFridgeItem {
      return RoomFridgeItem(
        item.id(),
        item.name(),
        item.expireTime(),
        item.presence()
      )
    }
  }
}