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
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerNotifications
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem

internal object ItemNotifications {

    private const val EXPIRING_NOTIFICATION_ID = 2345
    private const val EXPIRING_CHANNEL_ID = "fridge_expiring_reminders_channel_v1"

    private const val EXPIRED_NOTIFICATION_ID = 3456
    private const val EXPIRED_CHANNEL_ID = "fridge_expiration_reminders_channel_v1"

    internal const val NEEDED_NOTIFICATION_ID = 1948
    private const val NEEDED_CHANNEL_ID = "fridge_needed_reminders_channel_v1"

    @CheckResult
    @JvmStatic
    fun notifyNeeded(
        handler: NotificationHandler,
        context: Context,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ): Boolean {
        return ButlerNotifications.notify(
            NEEDED_NOTIFICATION_ID,
            handler,
            context,
            NEEDED_CHANNEL_ID,
            "Needed Reminders",
            "Reminders for items that you still need.",
            FridgeItem.Presence.NEED
        ) { builder ->
            val extra = ButlerNotifications.getExtraItems(items)
            return@notify builder
                .setContentTitle("Needed reminder for ${entry.name()}")
                .setContentText("You still need '${items.first().name()}' $extra")
                .build()
        }
    }

    @CheckResult
    @JvmStatic
    fun notifyExpiring(
        handler: NotificationHandler,
        context: Context,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ): Boolean {
        return ButlerNotifications.notify(
            EXPIRING_NOTIFICATION_ID,
            handler,
            context,
            EXPIRING_CHANNEL_ID,
            "Expiring Reminders",
            "Reminders for items that are going to expire soon",
            FridgeItem.Presence.HAVE
        ) { builder ->
            val extra =
                "${ButlerNotifications.getExtraItems(
                    items
                )} ${if (items.size == 1) "is" else "are"} about to expire."
            return@notify builder
                .setContentTitle("Expiration reminder for '${entry.name()}'")
                .setContentText("'${items.first().name()}' $extra")
                .build()
        }
    }

    @CheckResult
    @JvmStatic
    fun notifyExpired(
        handler: NotificationHandler,
        context: Context,
        entry: FridgeEntry,
        items: List<FridgeItem>
    ): Boolean {
        return ButlerNotifications.notify(
            EXPIRED_NOTIFICATION_ID,
            handler,
            context,
            EXPIRED_CHANNEL_ID,
            "Expired Reminders",
            "Reminders for items that have expired",
            FridgeItem.Presence.HAVE
        ) { builder ->
            return@notify builder
                .setContentTitle("Expired warning for '${entry.name()}'")
                .setContentText(
                    "'${items.first().name()}' ${ButlerNotifications.getExtraItems(
                        items
                    )} ${if (items.size == 1) "has" else "have"} passed expiration!"
                )
                .build()
        }
    }
}
