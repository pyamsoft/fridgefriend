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
import com.pyamsoft.fridge.butler.Notifications
import com.pyamsoft.fridge.butler.runner.needed.NeededNotifications
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone

internal object NearbyNotifications {

    private const val NEARBY_NOTIFICATION_ID = 1234

    private const val NEARBY_CHANNEL_ID = "fridge_nearby_reminders_channel_v1"

    @CheckResult
    private fun notifyNearby(
        handler: NotificationHandler,
        context: Context,
        storeName: String,
        items: List<FridgeItem>
    ): Boolean {
        // Plain needed notifications are overruled by location aware Nearby notifications
        Notifications.cancel(context.applicationContext, NeededNotifications.NEEDED_NOTIFICATION_ID)

        return ButlerNotifications.notify(
            NEARBY_NOTIFICATION_ID,
            handler,
            context,
            NEARBY_CHANNEL_ID,
            "Nearby Reminders",
            "Reminders for items that may be at locations nearby.",
            FridgeItem.Presence.NEED
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
    fun notifyNearby(
        handler: NotificationHandler,
        context: Context,
        store: NearbyStore,
        items: List<FridgeItem>
    ): Boolean {
        return notifyNearby(
            handler,
            context,
            store.name(),
            items
        )
    }

    @CheckResult
    @JvmStatic
    fun notifyNearby(
        handler: NotificationHandler,
        context: Context,
        zone: NearbyZone,
        items: List<FridgeItem>
    ): Boolean {
        return notifyNearby(
            handler,
            context,
            zone.name(),
            items
        )
    }
}
