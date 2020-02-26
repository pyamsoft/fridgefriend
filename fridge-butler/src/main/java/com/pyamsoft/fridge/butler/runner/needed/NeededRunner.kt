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

package com.pyamsoft.fridge.butler.runner.needed

import android.content.Context
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.runner.FridgeRunner
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

internal class NeededRunner @Inject internal constructor(
    private val context: Context,
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
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

    private suspend fun notifyForEntry(
        params: Parameters,
        preferences: ButlerPreferences,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ) {
        val neededItems = mutableListOf<FridgeItem>()

        for (item in items.filterNot { it.isArchived() }) {
            if (item.presence() == NEED) {
                Timber.w("Still need ${item.name()}")
                neededItems.add(item)
            }
        }

        val now = Calendar.getInstance()
        if (neededItems.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNeeded()
            if (now.isAllowedToNotify(params.forceNotification, lastTime)) {
                Timber.d("Notify user about items still needed")
                notification { handler ->
                    val notified = NeededNotifications.notifyNeeded(
                        handler,
                        context.applicationContext,
                        entry,
                        neededItems
                    )
                    if (notified) {
                        preferences.markNotificationExpiringSoon(now)
                    }
                }
            }
        }
    }

    override suspend fun reschedule(butler: Butler, params: Parameters) {
        val p = Butler.Parameters(
            forceNotification = params.forceNotification
        )
        butler.scheduleRemindNeeded(p)
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: Parameters
    ) = coroutineScope {
        withFridgeData { entry, items -> notifyForEntry(params, preferences, entry, items) }
    }
}
