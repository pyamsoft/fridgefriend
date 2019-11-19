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

package com.pyamsoft.fridge.butler.workmanager.expiration

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.FridgeWorker
import com.pyamsoft.fridge.db.FridgeItemPreferences
import com.pyamsoft.fridge.db.cleanMidnight
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.isArchived
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS

internal class ExpirationWorker internal constructor(
    context: Context,
    params: WorkerParameters
) : FridgeWorker(context, params) {

    override fun reschedule(butler: Butler) {
        butler.remindExpiration(RECURRING_INTERVAL, HOURS)
    }

    private fun notifyForEntry(
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
            if (now.isAllowedToNotify(lastTime, RECURRING_INTERVAL)) {
                Timber.d("Notify user about items expiring soon")
                notification { handler, foregroundState ->
                    val notified = ExpirationNotifications.notifyExpiring(
                        handler, foregroundState, applicationContext, entry, now, expiringItems
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
            if (now.isAllowedToNotify(lastTime, RECURRING_INTERVAL)) {
                Timber.d("Notify user about items expired")
                notification { handler, foregroundState ->
                    val notified = ExpirationNotifications.notifyExpired(
                        handler, foregroundState, applicationContext, entry, now, expiredItems
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

    companion object {

        private const val RECURRING_INTERVAL = 3L
    }
}
