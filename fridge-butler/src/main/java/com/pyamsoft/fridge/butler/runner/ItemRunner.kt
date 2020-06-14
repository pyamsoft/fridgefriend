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

package com.pyamsoft.fridge.butler.runner

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
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
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class ItemRunner @Inject internal constructor(
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    private val fridgeItemPreferences: FridgeItemPreferences,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao
) : FridgeRunner<ItemParameters>(
    handler,
    butler,
    notificationPreferences,
    butlerPreferences,
    fridgeEntryQueryDao,
    fridgeItemQueryDao
) {

    private suspend fun notifyNeeded(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNeeded()
            if (now.isAllowedToNotify(params.forceNotifyNeeded, lastTime)) {
                Timber.d("Notify user about items still needed")
                notification { handler ->
                    if (handler.notifyNeeded(entry, items)) {
                        preferences.markNotificationNeeded(now)
                    }
                }
            }
        }
    }

    private suspend fun notifyExpiringSoon(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpiringSoon()
            if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
                Timber.d("Notify user about items expiring soon")
                notification { handler ->
                    if (handler.notifyExpiring(entry, items)) {
                        preferences.markNotificationExpiringSoon(now)
                    }
                }
            }
        }
    }

    private suspend fun notifyExpired(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: ItemParameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpired()
            if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
                Timber.d("Notify user about items expired")
                notification { handler ->
                    if (handler.notifyExpired(entry, items)) {
                        preferences.markNotificationExpired(now)
                    }
                }
            }
        }
    }

    private suspend fun notifyForEntry(
        params: ItemParameters,
        preferences: ButlerPreferences,
        current: Calendar,
        later: Calendar,
        isSameDayExpired: Boolean,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ) {
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

        val now = today()
        notifyNeeded(neededItems, entry, now, preferences, params)
        notifyExpiringSoon(expiringItems, entry, now, preferences, params)
        notifyExpired(expiredItems, entry, now, preferences, params)
    }

    override suspend fun reschedule(butler: Butler, params: ItemParameters) {
        butler.scheduleRemindItems(
            ItemParameters(
                forceNotifyNeeded = false,
                forceNotifyExpiring = false
            )
        )
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: ItemParameters
    ) = coroutineScope {
        val today = today().cleanMidnight()
        val later = today().daysLaterMidnight(fridgeItemPreferences.getExpiringSoonRange())
        val isSameDayExpired = fridgeItemPreferences.isSameDayExpired()

        withFridgeData { entry, items ->
            notifyForEntry(params, preferences, today, later, isSameDayExpired, entry, items)
        }
    }
}
