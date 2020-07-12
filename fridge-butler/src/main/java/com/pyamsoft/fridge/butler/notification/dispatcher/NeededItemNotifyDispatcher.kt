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

package com.pyamsoft.fridge.butler.notification.dispatcher

import android.app.Activity
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.pyamsoft.fridge.butler.R
import com.pyamsoft.fridge.butler.notification.NeededItemNotifyData
import com.pyamsoft.fridge.butler.notification.NotificationChannelInfo
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NeededItemNotifyDispatcher @Inject internal constructor(
    context: Context,
    activityClass: Class<out Activity>
) : ItemNotifyDispatcher<NeededItemNotifyData>(
    context,
    activityClass = activityClass,
    channel = NotificationChannelInfo(
        id = CHANNEL_ID,
        title = "Shopping Reminders",
        description = "Reminders for items that you still need."
    )
) {

    companion object {
        private const val CHANNEL_ID = "fridge_needed_reminders_channel_v1"
    }

    override fun canShow(notification: NotifyData): Boolean {
        return notification is NeededItemNotifyData
    }

    override fun onBuildNotification(
        id: NotifyId,
        notification: NeededItemNotifyData,
        builder: NotificationCompat.Builder
    ): Notification {
        builder.apply {
            setSmallIcon(R.drawable.ic_shopping_cart_24dp)
            setContentIntent(createContentIntent(id, FridgeItem.Presence.NEED))
            setContentTitle(buildSpannedString {
                bold { append("Shopping reminder") }
                append(" for ")
                bold { append(notification.entry.name()) }
            })

            val text = buildSpannedString {
                append("You still ")
                bold { append("need to shop") }
                append(" for ")
                bold { append("${notification.items.size}") }
                append(" items")
            }
            setContentText(text)
            setStyle(
                createBigTextStyle(
                    text,
                    notification.items,
                    isExpired = false,
                    isExpiringSoon = false
                )
            )
        }
        return builder.build()
    }
}