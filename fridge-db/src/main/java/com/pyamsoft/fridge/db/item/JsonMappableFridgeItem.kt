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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableFridgeItem internal constructor(
    internal val id: FridgeItem.Id,
    internal val entryId: FridgeEntry.Id,
    internal val name: String,
    internal val count: Int,
    internal val createdTime: Date,
    internal val purchasedTime: Date?,
    internal val expireTime: Date?,
    internal val presence: Presence,
    internal val consumptionDate: Date?,
    internal val spoiledDate: Date?,
    internal val categoryId: FridgeCategory.Id?,
    internal val isReal: Boolean
) : FridgeItem {

    override fun id(): FridgeItem.Id {
        return id
    }

    override fun entryId(): FridgeEntry.Id {
        return entryId
    }

    override fun name(): String {
        return name
    }

    override fun count(): Int {
        return count
    }

    override fun createdTime(): Date {
        return createdTime
    }

    override fun purchaseTime(): Date? {
        return purchasedTime
    }

    override fun expireTime(): Date? {
        return expireTime
    }

    override fun presence(): Presence {
        return presence
    }

    override fun categoryId(): FridgeCategory.Id? {
        return categoryId
    }

    override fun isReal(): Boolean {
        return isReal
    }

    override fun consumptionDate(): Date? {
        require(isReal()) { "Cannot query consumptionDate on non-real item: $this" }
        return consumptionDate
    }

    override fun invalidateConsumption(): FridgeItem {
        require(isReal()) { "Cannot invalidate consumptionDate on non-real item: $this" }
        return this.copy(consumptionDate = null)
    }

    override fun isConsumed(): Boolean {
        require(isReal()) { "Cannot query consumptionDate on non-real item: $this" }
        return consumptionDate != null
    }

    override fun spoiledDate(): Date? {
        require(isReal()) { "Cannot query spoiledDate on non-real item: $this" }
        return spoiledDate
    }

    override fun invalidateSpoiled(): FridgeItem {
        require(isReal()) { "Cannot invalidate spoiledDate on non-real item: $this" }
        return this.copy(spoiledDate = null)
    }

    override fun isSpoiled(): Boolean {
        require(isReal()) { "Cannot query spoiledDate on non-real item: $this" }
        return spoiledDate != null
    }

    override fun isEmpty(): Boolean {
        return id().isEmpty()
    }

    override fun name(name: String): FridgeItem {
        return this.copy(name = name.trim())
    }

    override fun count(count: Int): FridgeItem {
        return this.copy(count = count)
    }

    override fun expireTime(expireTime: Date): FridgeItem {
        return this.copy(expireTime = expireTime)
    }

    override fun invalidateExpiration(): FridgeItem {
        return this.copy(expireTime = null)
    }

    override fun purchaseTime(purchaseTime: Date): FridgeItem {
        return this.copy(purchasedTime = purchaseTime)
    }

    override fun invalidatePurchase(): FridgeItem {
        return this.copy(purchasedTime = null)
    }

    override fun presence(presence: Presence): FridgeItem {
        return this.copy(presence = presence)
    }

    override fun makeReal(): FridgeItem {
        return this.copy(isReal = true)
    }

    override fun consume(date: Date): FridgeItem {
        require(isReal()) { "Cannot consume non-real item: $this" }
        return this.copy(consumptionDate = date)
    }

    override fun spoil(date: Date): FridgeItem {
        require(isReal()) { "Cannot spoil non-real item: $this" }
        return this.copy(spoiledDate = date)
    }

    override fun invalidateCategoryId(): FridgeItem {
        return this.copy(categoryId = null)
    }

    override fun categoryId(id: FridgeCategory.Id): FridgeItem {
        return this.copy(categoryId = id)
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun from(item: FridgeItem): JsonMappableFridgeItem {
            return if (item is JsonMappableFridgeItem) item else {
                JsonMappableFridgeItem(
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
                    item.categoryId(),
                    item.isReal()
                )
            }
        }
    }
}
