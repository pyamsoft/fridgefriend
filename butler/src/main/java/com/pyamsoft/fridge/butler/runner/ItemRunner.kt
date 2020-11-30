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
 */

package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.order.Order
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.cleanMidnight
import com.pyamsoft.fridge.db.item.daysLaterMidnight
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.item.isExpired
import com.pyamsoft.fridge.db.item.isExpiringSoon
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ItemRunner @Inject internal constructor(
    private val butler: Butler,
    private val fridgeItemPreferences: FridgeItemPreferences,
    handler: NotificationHandler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao
) : FridgeRunner<ItemParameters>(
    handler,
    notificationPreferences,
    butlerPreferences,
    fridgeEntryQueryDao,
    fridgeItemQueryDao
) {

    override suspend fun onReschedule(order: Order) {
        Timber.d("Rescheduling order: $order")
        butler.scheduleOrder(order)
    }

    @CheckResult
    private suspend fun notifyNeeded(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ): Boolean {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNeeded()
            if (now.isAllowedToNotify(params.forceNotifyNeeded, lastTime)) {
                Timber.d("Notify user about items still needed")
                return notification { notifyNeeded(entry, items) }
            }
        }

        return false
    }

    @CheckResult
    private suspend fun notifyExpiringSoon(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ): Boolean {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpiringSoon()
            if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
                Timber.d("Notify user about items expiring soon")
                return notification { notifyExpiring(entry, items) }
            }
        }

        return false
    }

    @CheckResult
    private suspend fun notifyExpired(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ): Boolean {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpired()
            if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
                Timber.d("Notify user about items expired")
                return notification { notifyExpired(entry, items) }
            }
        }

        return false
    }

    @CheckResult
    private suspend fun notifyForEntry(
        params: ItemParameters,
        preferences: ButlerPreferences,
        now: Calendar,
        current: Calendar,
        later: Calendar,
        isSameDayExpired: Boolean,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ): NotifyResults {
        val neededItems = mutableListOf<FridgeItem>()
        val expiringItems = mutableListOf<FridgeItem>()
        val expiredItems = mutableListOf<FridgeItem>()

        items.filterNot { it.isArchived() }.forEach { item ->
            when (item.presence()) {
                HAVE -> {
                    if (item.isExpired(current, isSameDayExpired)) {
                        Timber.w("${item.name()} expired!")
                        expiredItems.add(item)
                    } else {
                        if (item.isExpiringSoon(current, later, isSameDayExpired)) {
                            Timber.w("${item.name()} is expiring soon!")
                            expiringItems.add(item)
                        }
                    }
                }
                NEED -> {
                    Timber.w("Still need ${item.name()}")
                    neededItems.add(item)
                }
            }
        }

        val needed = notifyNeeded(neededItems, entry, now, preferences, params)
        val expiring = notifyExpiringSoon(expiringItems, entry, now, preferences, params)
        val expired = notifyExpired(expiredItems, entry, now, preferences, params)
        return NotifyResults(entry.id(), needed, expiring, expired, nearby = false)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: ItemParameters
    ) = coroutineScope {
        val now = today()
        val today = today().cleanMidnight()
        val later = today().daysLaterMidnight(fridgeItemPreferences.getExpiringSoonRange())
        val isSameDayExpired = fridgeItemPreferences.isSameDayExpired()

        withFridgeData { entry, items ->
            notifyForEntry(params, preferences, now, today, later, isSameDayExpired, entry, items)
        }.let { results ->
            results.firstOrNull { it.needed }?.let {
                preferences.markNotificationNeeded(now)
            }

            results.firstOrNull { it.expiring }?.let {
                preferences.markNotificationExpiringSoon(now)
            }

            results.firstOrNull { it.expired }?.let {
                preferences.markNotificationExpired(now)
            }

            // Unit
            return@coroutineScope
        }
    }
}
