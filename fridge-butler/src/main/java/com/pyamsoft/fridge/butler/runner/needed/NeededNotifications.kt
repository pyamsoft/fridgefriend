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
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerNotifications
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem

internal object NeededNotifications {

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
}
