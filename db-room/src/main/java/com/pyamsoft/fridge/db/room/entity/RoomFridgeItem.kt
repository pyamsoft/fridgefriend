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
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import java.util.Date

@Entity(
    tableName = RoomFridgeItem.TABLE_NAME,
    foreignKeys =
        [
            ForeignKey(
                entity = RoomFridgeEntry::class,
                parentColumns = arrayOf(RoomFridgeEntry.COLUMN_ID),
                childColumns = arrayOf(RoomFridgeItem.COLUMN_ENTRY_ID),
                onDelete = ForeignKey.CASCADE)])
internal data class RoomFridgeItem
internal constructor(
    @JvmField @PrimaryKey @ColumnInfo(name = COLUMN_ID) val id: FridgeItem.Id,
    @JvmField @ColumnInfo(name = COLUMN_ENTRY_ID, index = true) val entryId: FridgeEntry.Id,
    @JvmField @ColumnInfo(name = COLUMN_NAME) val name: String,
    @JvmField @ColumnInfo(name = COLUMN_COUNT) val count: Int,
    @JvmField @ColumnInfo(name = COLUMN_CREATED_TIME) val createdTime: Date,
    @JvmField @ColumnInfo(name = COLUMN_PURCHASE_TIME) val purchaseTime: Date?,
    @JvmField @ColumnInfo(name = COLUMN_EXPIRE_TIME) val expireTime: Date?,
    @JvmField @ColumnInfo(name = COLUMN_PRESENCE) val presence: Presence,
    @JvmField @ColumnInfo(name = COLUMN_CONSUMED) val consumedTime: Date?,
    @JvmField @ColumnInfo(name = COLUMN_SPOILED) val spoiledTime: Date?,
    @JvmField @ColumnInfo(name = COLUMN_CATEGORY) val categoryId: FridgeCategory.Id?,
) : FridgeItem {

  @Ignore
  override fun id(): FridgeItem.Id {
    return id
  }

  @Ignore
  override fun entryId(): FridgeEntry.Id {
    return entryId
  }

  @Ignore
  override fun name(): String {
    return name
  }

  @Ignore
  override fun count(): Int {
    return count
  }

  @Ignore
  override fun createdTime(): Date {
    return createdTime
  }

  @Ignore
  override fun purchaseTime(): Date? {
    return purchaseTime
  }

  @Ignore
  override fun expireTime(): Date? {
    return expireTime
  }

  @Ignore
  override fun presence(): Presence {
    return presence
  }

  @Ignore
  override fun categoryId(): FridgeCategory.Id? {
    return categoryId
  }

  @Ignore
  override fun isReal(): Boolean {
    return true
  }

  @Ignore
  override fun consumptionDate(): Date? {
    require(isReal()) { "Cannot query consumedDate on non-real item: $this" }
    return consumedTime
  }

  @Ignore
  override fun invalidateConsumption(): FridgeItem {
    require(isReal()) { "Cannot invalidate consumedDate on non-real item: $this" }
    return FridgeItem.create(this, consumptionDate = null, isReal = isReal())
  }

  @Ignore
  override fun isConsumed(): Boolean {
    require(isReal()) { "Cannot query consumedDate on non-real item: $this" }
    return consumedTime != null
  }

  @Ignore
  override fun spoiledDate(): Date? {
    require(isReal()) { "Cannot query spoiledDate on non-real item: $this" }
    return spoiledTime
  }

  @Ignore
  override fun invalidateSpoiled(): FridgeItem {
    require(isReal()) { "Cannot invalidate spoiledDate on non-real item: $this" }
    return FridgeItem.create(this, spoiledDate = null, isReal = isReal())
  }

  @Ignore
  override fun isSpoiled(): Boolean {
    require(isReal()) { "Cannot query spoiledDate on non-real item: $this" }
    return spoiledTime != null
  }

  @Ignore
  override fun isEmpty(): Boolean {
    return id().isEmpty()
  }

  @Ignore
  override fun migrateTo(entryId: FridgeEntry.Id): FridgeItem {
    return FridgeItem.create(this, entryId = entryId, isReal = isReal())
  }

  @Ignore
  override fun name(name: String): FridgeItem {
    return FridgeItem.create(this, name = name.trim(), isReal = isReal())
  }

  @Ignore
  override fun count(count: Int): FridgeItem {
    return FridgeItem.create(this, count = count, isReal = isReal())
  }

  @Ignore
  override fun expireTime(expireTime: Date): FridgeItem {
    return FridgeItem.create(this, expireTime = expireTime, isReal = isReal())
  }

  @Ignore
  override fun invalidateExpiration(): FridgeItem {
    return FridgeItem.create(this, expireTime = null, isReal = isReal())
  }

  @Ignore
  override fun purchaseTime(purchaseTime: Date): FridgeItem {
    return FridgeItem.create(this, purchaseTime = purchaseTime, isReal = isReal())
  }

  @Ignore
  override fun invalidatePurchase(): FridgeItem {
    return FridgeItem.create(this, purchaseTime = null, isReal = isReal())
  }

  @Ignore
  override fun presence(presence: Presence): FridgeItem {
    return FridgeItem.create(this, presence = presence, isReal = isReal())
  }

  @Ignore
  override fun makeReal(): FridgeItem {
    return FridgeItem.create(this, isReal = true)
  }

  @Ignore
  override fun consume(date: Date): FridgeItem {
    require(isReal()) { "Cannot consume non-real item: $this" }
    return FridgeItem.create(this, consumptionDate = date, isReal = isReal())
  }

  @Ignore
  override fun spoil(date: Date): FridgeItem {
    require(isReal()) { "Cannot spoil non-real item: $this" }
    return FridgeItem.create(this, spoiledDate = date, isReal = isReal())
  }

  @Ignore
  override fun invalidateCategoryId(): FridgeItem {
    return FridgeItem.create(this, categoryId = null, isReal = isReal())
  }

  @Ignore
  override fun categoryId(id: FridgeCategory.Id): FridgeItem {
    return FridgeItem.create(this, categoryId = categoryId, isReal = isReal())
  }

  companion object {

    @Ignore internal const val TABLE_NAME = "room_fridge_item_table"

    @Ignore internal const val COLUMN_ID = "_id"

    @Ignore internal const val COLUMN_ENTRY_ID = "entry_id"

    @Ignore internal const val COLUMN_NAME = "name"

    @Ignore internal const val COLUMN_COUNT = "count"

    @Ignore internal const val COLUMN_CREATED_TIME = "created_time"

    @Ignore internal const val COLUMN_PURCHASE_TIME = "purchase_time"

    @Ignore internal const val COLUMN_EXPIRE_TIME = "expire_time"

    @Ignore internal const val COLUMN_PRESENCE = "presence"

    @Ignore internal const val COLUMN_CONSUMED = "consumed"

    @Ignore internal const val COLUMN_SPOILED = "spoiled"

    @Ignore internal const val COLUMN_CATEGORY = "category"

    @Ignore
    @JvmStatic
    @CheckResult
    internal fun create(item: FridgeItem): RoomFridgeItem {
      return if (item is RoomFridgeItem) item
      else {
        RoomFridgeItem(
            item.id(),
            item.entryId(),
            item.name(),
            item.count(),
            item.createdTime(),
            item.purchaseTime(),
            item.expireTime(),
            item.presence(),
            item.consumptionDate(),
            item.spoiledDate(),
            item.categoryId())
      }
    }
  }
}
