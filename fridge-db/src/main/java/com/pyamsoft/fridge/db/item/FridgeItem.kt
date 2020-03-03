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
import com.pyamsoft.fridge.core.IdGenerator
import com.pyamsoft.fridge.db.EmptyModel
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import java.util.Calendar
import java.util.Date

interface FridgeItem : EmptyModel<FridgeItem> {

    @CheckResult
    fun id(): Id

    @CheckResult
    fun entryId(): FridgeEntry.Id

    @CheckResult
    fun count(): Int

    @CheckResult
    fun purchaseTime(): Date?

    @CheckResult
    fun expireTime(): Date?

    @CheckResult
    fun presence(): Presence

    @CheckResult
    fun categoryId(): FridgeCategory.Id?

    @CheckResult
    fun isReal(): Boolean

    @CheckResult
    fun count(count: Int): FridgeItem

    @CheckResult
    fun expireTime(expireTime: Date): FridgeItem

    @CheckResult
    fun invalidateExpiration(): FridgeItem

    @CheckResult
    fun purchaseTime(purchaseTime: Date): FridgeItem

    @CheckResult
    fun invalidatePurchase(): FridgeItem

    @CheckResult
    fun presence(presence: Presence): FridgeItem

    @CheckResult
    fun isConsumed(): Boolean

    @CheckResult
    fun consumptionDate(): Date?

    @CheckResult
    fun consume(date: Date): FridgeItem

    @CheckResult
    fun invalidateConsumption(): FridgeItem

    @CheckResult
    fun isSpoiled(): Boolean

    @CheckResult
    fun spoiledDate(): Date?

    @CheckResult
    fun invalidateSpoiled(): FridgeItem

    @CheckResult
    fun spoil(date: Date): FridgeItem

    @CheckResult
    fun invalidateCategoryId(): FridgeItem

    @CheckResult
    fun categoryId(id: FridgeCategory.Id): FridgeItem

    @CheckResult
    fun makeReal(): FridgeItem

    data class Id(val id: String) {

        @CheckResult
        fun isEmpty(): Boolean {
            return id.isBlank()
        }

        companion object {

            @JvmField
            val EMPTY = Id("")
        }
    }

    enum class Presence {
        HAVE,
        NEED;

        @CheckResult
        fun flip(): Presence {
            return if (this == NEED) HAVE else NEED
        }

        companion object {

            const val KEY = "key_presence"
        }
    }

    companion object {

        private val EMPTY_ITEM = create(Id.EMPTY, FridgeEntry.Id.EMPTY, Presence.NEED)

        @CheckResult
        fun isValidName(name: String): Boolean {
            return name.isNotBlank()
        }

        @CheckResult
        fun empty(): FridgeItem {
            return EMPTY_ITEM
        }

        @CheckResult
        @JvmOverloads
        fun create(
            id: Id = Id(IdGenerator.generate()),
            entryId: FridgeEntry.Id,
            presence: Presence
        ): FridgeItem {
            return JsonMappableFridgeItem(
                id = id,
                entryId = entryId,
                name = "",
                count = 1,
                createdTime = Date(),
                purchasedTime = null,
                expireTime = null,
                presence = presence,
                categoryId = null,
                consumptionDate = null,
                spoiledDate = null,
                isReal = false
            )
        }

        @CheckResult
        @JvmOverloads
        fun create(
            item: FridgeItem,
            name: String = item.name(),
            count: Int = item.count(),
            createdTime: Date = item.createdTime(),
            expireTime: Date? = item.expireTime(),
            purchaseTime: Date? = item.purchaseTime(),
            presence: Presence = item.presence(),
            consumptionDate: Date? = item.consumptionDate(),
            spoiledDate: Date? = item.spoiledDate(),
            categoryId: FridgeCategory.Id? = item.categoryId(),
            isReal: Boolean
        ): FridgeItem {
            return JsonMappableFridgeItem(
                item.id(),
                item.entryId(),
                name,
                count,
                createdTime,
                purchaseTime,
                expireTime,
                presence,
                consumptionDate,
                spoiledDate,
                categoryId,
                isReal
            )
        }
    }
}

@CheckResult
fun FridgeItem.isArchived(): Boolean {
    return this.isConsumed() || this.isSpoiled()
}

@CheckResult
fun FridgeItem.isExpired(today: Calendar, countSameDayAsExpired: Boolean): Boolean {
    val expireTime = this.expireTime() ?: return false

    // Clean Y/M/D only
    val expiration = Calendar.getInstance()
        .also { it.time = expireTime }
        .cleanMidnight()

    val midnightToday = today.cleanMidnight()
    if (expiration.before(midnightToday)) {
        return true
    }

    if (countSameDayAsExpired) {
        return expiration == midnightToday
    }

    return false
}

@CheckResult
fun FridgeItem.isExpiringSoon(
    today: Calendar,
    tomorrow: Calendar,
    countSameDayAsExpired: Boolean
): Boolean {
    val expireTime = this.expireTime() ?: return false

    if (this.isExpired(today, countSameDayAsExpired)) {
        return false
    }

    // Clean Y/M/D only
    val expiration = Calendar.getInstance()
        .also { it.time = expireTime }
        .cleanMidnight()

    val midnightToday = today.cleanMidnight()
    val midnightTomorrow = tomorrow.cleanMidnight()
    return expiration.before(midnightTomorrow) || expiration == midnightTomorrow || expiration == midnightToday
}
