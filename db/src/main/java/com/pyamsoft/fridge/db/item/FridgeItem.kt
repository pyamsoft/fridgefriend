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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.IdGenerator
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.EmptyModel
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import java.util.Calendar
import java.util.Date
import kotlin.math.roundToLong

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
    fun migrateTo(entryId: FridgeEntry.Id): FridgeItem

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
    }

    companion object {

        const val MARK_EXPIRED = "Ⓧ"
        const val MARK_EXPIRING_SOON = "▲"
        const val MARK_FRESH = "⭘"

        @CheckResult
        fun isValidName(name: String): Boolean {
            return name.isNotBlank()
        }

        @CheckResult
        @JvmOverloads
        fun create(
            id: Id = Id(IdGenerator.generate()),
            entryId: FridgeEntry.Id,
            presence: Presence,
        ): FridgeItem {
            return JsonMappableFridgeItem(
                id = id,
                entryId = entryId,
                name = "",
                count = 1,
                createdTime = currentDate(),
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
            entryId: FridgeEntry.Id = item.entryId(),
            name: String = item.name(),
            count: Int = item.count(),
            createdTime: Date = item.createdTime(),
            expireTime: Date? = item.expireTime(),
            purchaseTime: Date? = item.purchaseTime(),
            presence: Presence = item.presence(),
            consumptionDate: Date? = item.consumptionDate(),
            spoiledDate: Date? = item.spoiledDate(),
            categoryId: FridgeCategory.Id? = item.categoryId(),
            isReal: Boolean,
        ): FridgeItem {
            return JsonMappableFridgeItem(
                item.id(),
                entryId,
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
    return if (this.isReal()) this.isConsumed() || this.isSpoiled() else false
}

@CheckResult
fun FridgeItem.isExpired(date: Calendar, countSameDayAsExpired: Boolean): Boolean {
    val expireTime = this.expireTime() ?: return false

    // Clean Y/M/D only
    val expiration = today()
        .also { it.time = expireTime }
        .cleanMidnight()

    val midnightToday = date.cleanMidnight()
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
    date: Calendar,
    later: Calendar,
    countSameDayAsExpired: Boolean,
): Boolean {
    val expireTime = this.expireTime() ?: return false

    if (this.isExpired(date, countSameDayAsExpired)) {
        return false
    }

    // Clean Y/M/D only
    val expiration = today()
        .also { it.time = expireTime }
        .cleanMidnight()

    val midnightToday = date.cleanMidnight()
    val midnightLater = later.cleanMidnight()
    return expiration.before(midnightLater) || expiration == midnightLater || expiration == midnightToday
}

@CheckResult
private fun getDaysToTime(nowTime: Long, expireTime: Long): Long {
    val difference = expireTime - nowTime
    val seconds = (difference.toFloat() / 1000L).roundToLong()
    val minutes = (seconds.toFloat() / 60L).roundToLong()
    val hours = (minutes.toFloat() / 60L).roundToLong()
    return (hours.toFloat() / 24L).roundToLong()
}

private const val WEEK_LIMIT = 10

@CheckResult
fun FridgeItem.getExpiringSoonMessage(now: Calendar): String {
    val todayTime = now.timeInMillis
    val expiringTime = requireNotNull(this.expireTime()).time
    val days = getDaysToTime(todayTime, expiringTime)

    require(days >= 0)
    return if (days == 0L) "expires today" else {
        if (days < 7) {
            "will expire in $days ${if (days == 1L) "day" else "days"}"
        } else {
            val weeks = days / 7L
            if (weeks < WEEK_LIMIT) {
                "will expire in $weeks ${if (weeks == 1L) "week" else "weeks"}"
            } else {
                "will not expire for a long time"
            }
        }
    }
}

@CheckResult
fun FridgeItem.getExpiredMessage(now: Calendar): String {
    val todayTime = now.timeInMillis
    val expiringTime = requireNotNull(this.expireTime()).time
    val days = getDaysToTime(expiringTime, todayTime)

    require(days >= 0)
    return if (days == 0L) "expires today" else {
        if (days < 7) {
            "expired $days ${if (days == 1L) "day" else "days"} ago"
        } else {
            val weeks = days / 7L
            if (weeks < WEEK_LIMIT) {
                "expired $weeks ${if (weeks == 1L) "week" else "weeks"} ago"
            } else {
                "expired a long time ago"
            }
        }
    }
}
