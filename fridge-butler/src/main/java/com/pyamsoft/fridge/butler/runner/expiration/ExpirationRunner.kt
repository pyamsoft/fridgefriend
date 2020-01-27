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

package com.pyamsoft.fridge.butler.runner.expiration

import android.content.Context
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.runner.FridgeRunner
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.cleanMidnight
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

internal class ExpirationRunner @Inject internal constructor(
    private val context: Context,
    handler: NotificationHandler,
    butler: Butler,
    butlerPreferences: ButlerPreferences,
    fridgeItemPreferences: FridgeItemPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao
) : FridgeRunner(
    handler,
    butler,
    butlerPreferences,
    fridgeItemPreferences,
    enforcer,
    fridgeEntryQueryDao,
    fridgeItemQueryDao
) {

    private suspend fun notifyForEntry(
        preferences: ButlerPreferences,
        today: Calendar,
        later: Calendar,
        isSameDayExpired: Boolean,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ) {
        val expiringItems = mutableListOf<FridgeItem>()
        val expiredItems = mutableListOf<FridgeItem>()
        val unknownExpirationItems = mutableListOf<FridgeItem>()

        for (item in items.filterNot { it.isArchived() }) {
            if (item.presence() == HAVE) {
                val expirationTime = item.expireTime()
                if (expirationTime != null) {

                    if (item.isExpired(today, isSameDayExpired)) {
                        Timber.w("${item.name()} expired!")
                        expiredItems.add(item)
                    } else {
                        if (item.isExpiringSoon(today, later, isSameDayExpired)) {
                            Timber.w("${item.name()} is expiring soon!")
                            expiringItems.add(item)
                        }
                    }
                } else {
                    unknownExpirationItems.add(item)
                }
            }
        }

        val now = Calendar.getInstance()
        if (expiringItems.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpiringSoon()
            if (now.isAllowedToNotify(lastTime)) {
                Timber.d("Notify user about items expiring soon")
                notification { handler ->
                    val notified =
                        ExpirationNotifications.notifyExpiring(
                            handler, context.applicationContext, entry, now, expiringItems
                        )
                    if (notified) {
                        preferences.markNotificationExpiringSoon(now)
                    }
                }
            } else {
                Timber.w("Do not notify user about expiring since last notification time too recent: $lastTime :: ${now.timeInMillis}")
            }
        }

        if (expiredItems.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeExpired()
            if (now.isAllowedToNotify(lastTime)) {
                Timber.d("Notify user about items expired")
                notification { handler ->
                    val notified =
                        ExpirationNotifications.notifyExpired(
                            handler, context.applicationContext, entry, now, expiredItems
                        )
                    if (notified) {
                        preferences.markNotificationExpired(now)
                    }
                }
            } else {
                Timber.w("Do not notify user about expired since last notification time too recent: $lastTime :: ${now.timeInMillis}")
            }
        }

        if (unknownExpirationItems.isNotEmpty()) {
            Timber.w("Butler cannot handle unknowns: $unknownExpirationItems")
        }
    }

    override fun reschedule(butler: Butler) {
        butler.scheduleRemindExpiration()
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    ) = coroutineScope {
        val today = Calendar.getInstance()
            .cleanMidnight()
        val later = Calendar.getInstance()
            .daysLaterMidnight(2)
        val isSameDayExpired = fridgeItemPreferences.isSameDayExpired()

        withFridgeData { entry, items ->
            notifyForEntry(preferences, today, later, isSameDayExpired, entry, items)
        }
    }
}
