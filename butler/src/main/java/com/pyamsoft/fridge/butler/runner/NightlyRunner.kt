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
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NightlyRunner @Inject internal constructor(
    private val butler: Butler,
    private val fridgeItemQueryDao: FridgeItemQueryDao,
    handler: NotificationHandler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
) : BaseRunner<EmptyParameters>(
    handler,
    notificationPreferences,
    butlerPreferences
) {

    override suspend fun onReschedule(order: Order) {
        Timber.d("Rescheduling order: $order")
        butler.scheduleOrder(order)
    }

    private suspend fun notifyNightly(
        items: List<FridgeItem>,
        now: Calendar,
        preferences: ButlerPreferences
    ) {
        if (items.isNotEmpty()) {
            val lastTime = preferences.getLastNotificationTimeNightly()
            if (now.isAllowedToNotify(false, lastTime) && isAtleastTime(now)) {
                Timber.d("Notify user about items nightly fridge cleanup")
                if (notification { notifyNightly() }) {
                    preferences.markNotificationNightly(now)
                }
            }
        }
    }

    override suspend fun performWork(
        preferences: ButlerPreferences,
        params: EmptyParameters
    ) = coroutineScope {
        val now = today()
        val items = fridgeItemQueryDao.query(false)
        notifyNightly(items.filter { it.presence() == HAVE }, now, preferences)
    }

    @CheckResult
    private fun isAtleastTime(now: Calendar): Boolean {
        val hour = now.get(Calendar.HOUR_OF_DAY)
        return hour >= EVENING_HOUR
    }

    companion object {
        // 8PM
        private const val EVENING_HOUR = 8 + 12
    }
}
