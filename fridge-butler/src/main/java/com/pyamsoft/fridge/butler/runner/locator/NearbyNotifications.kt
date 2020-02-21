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

package com.pyamsoft.fridge.butler.runner.locator

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerNotifications
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import java.util.Calendar

internal object NearbyNotifications {

    private const val NEEDED_NOTIFICATION_ID = 1234

    private const val NEEDED_CHANNEL_ID = "fridge_needed_reminders_channel_v1"

    @CheckResult
    private fun notifyNeeded(
        handler: NotificationHandler,
        context: Context,
        storeName: String,
        now: Calendar,
        items: List<FridgeItem>
    ): Boolean {
        return ButlerNotifications.notify(
            NEEDED_NOTIFICATION_ID,
            handler,
            context,
            NEEDED_CHANNEL_ID,
            "Needed Reminders",
            "Reminders for items that you still need to purchase.",
            FridgeItem.Presence.NEED,
            now
        ) { builder ->
            val extra = ButlerNotifications.getExtraItems(items)
            return@notify builder
                .setContentTitle("Nearby Reminder for $storeName")
                .setContentText("You still need '${items.first().name()}' $extra")
                .build()
        }
    }

    @CheckResult
    @JvmStatic
    fun notifyNeeded(
        handler: NotificationHandler,
        context: Context,
        store: NearbyStore,
        now: Calendar,
        items: List<FridgeItem>
    ): Boolean {
        return notifyNeeded(
            handler,
            context,
            store.name(),
            now,
            items
        )
    }

    @CheckResult
    @JvmStatic
    fun notifyNeeded(
        handler: NotificationHandler,
        context: Context,
        zone: NearbyZone,
        now: Calendar,
        items: List<FridgeItem>
    ): Boolean {
        return notifyNeeded(
            handler,
            context,
            zone.name(),
            now,
            items
        )
    }
}