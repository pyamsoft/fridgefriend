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

package com.pyamsoft.fridge.butler.runner.item

import android.content.Context
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.runner.FridgeRunner
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
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Calendar
import javax.inject.Inject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

internal class ItemRunner @Inject internal constructor(
    private val context: Context,
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    private val fridgeItemPreferences: FridgeItemPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao
) : FridgeRunner(
    handler,
    butler,
    notificationPreferences,
    butlerPreferences,
    enforcer,
    fridgeEntryQueryDao,
    fridgeItemQueryDao
) {

    private suspend fun notifyNeeded(
        items: List<FridgeItem>,
        entry: FridgeEntry,
        now: Calendar,
        preferences: ButlerPreferences,
        params: Parameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNeeded()
            if (now.isAllowedToNotify(params.forceNotification, lastTime)) {
                Timber.d("Notify user about items still needed")
                notification { handler ->
                    val notified = ItemNotifications.notifyNeeded(
                        handler,
                        context.applicationContext,
                        entry,
                        items
                    )
                    if (notified) {
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
        params: Parameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpiringSoon()
            if (now.isAllowedToNotify(params.forceNotification, lastTime)) {
                Timber.d("Notify user about items expiring soon")
                notification { handler ->
                    val notified = ItemNotifications.notifyExpiring(
                        handler,
                        context.applicationContext,
                        entry,
                        items
                    )
                    if (notified) {
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
        params: Parameters
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpired()
            if (now.isAllowedToNotify(params.forceNotification, lastTime)) {
                Timber.d("Notify user about items expired")
                notification { handler ->
                    val notified = ItemNotifications.notifyExpired(
                        handler,
                        context.applicationContext,
                        entry,
                        items
                    )
                    if (notified) {
                        preferences.markNotificationExpired(now)
                    }
                }
            }
        }
    }

    private suspend fun notifyForEntry(
        params: Parameters,
        preferences: ButlerPreferences,
        today: Calendar,
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
                    if (item.isExpired(today, isSameDayExpired)) {
                        Timber.w("${item.name()} expired!")
                        expiredItems.add(item)
                    } else {
                        if (item.isExpiringSoon(today, later, isSameDayExpired)) {
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

        val now = Calendar.getInstance()
        notifyNeeded(neededItems, entry, now, preferences, params)
        notifyExpiringSoon(expiringItems, entry, now, preferences, params)
        notifyExpired(expiredItems, entry, now, preferences, params)
    }

    override suspend fun reschedule(butler: Butler, params: Parameters) {
        val p = Butler.Parameters(
            forceNotification = params.forceNotification
        )
        butler.scheduleRemindItems(p)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: Parameters
    ) = coroutineScope {
        val today = Calendar.getInstance()
            .cleanMidnight()
        val later = Calendar.getInstance()
            .daysLaterMidnight(2)
        val isSameDayExpired = fridgeItemPreferences.isSameDayExpired()

        withFridgeData { entry, items ->
            notifyForEntry(params, preferences, today, later, isSameDayExpired, entry, items)
        }
    }
}
