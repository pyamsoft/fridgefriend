/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.butler.notification.dispatcher

import android.app.Activity
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.pyamsoft.fridge.butler.notification.ExpiredItemNotifyData
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ExpiredItemNotifyDispatcher @Inject internal constructor(
    context: Context,
    activityClass: Class<out Activity>
) : ItemNotifyDispatcher<ExpiredItemNotifyData>(
    context,
    activityClass = activityClass
) {

    override fun canShow(notification: NotifyData): Boolean {
        return notification is ExpiredItemNotifyData
    }

    override fun onBuildNotification(
        id: NotifyId,
        notification: ExpiredItemNotifyData,
        builder: NotificationCompat.Builder
    ): Notification {
        builder.apply {
            setSmallIcon(R.drawable.ic_spoiled_24dp)
            setContentTitle(buildSpannedString {
                bold { append("Expiration warning") }
                append(" for ")
                bold { append(notification.entry.name()) }
            })

            val text = buildSpannedString {
                bold { append("${notification.items.size}") }
                append(" items have ")
                bold { append("passed expiration!") }
            }
            setContentText(text)
            setStyle(
                createBigTextStyle(
                    text,
                    notification.items,
                    isExpired = true,
                    isExpiringSoon = false
                )
            )

            setContentIntent(createContentIntent(id) {
                putExtra(NotificationHandler.KEY_ENTRY_ID, notification.entry.id().id)
                putExtra(NotificationHandler.KEY_PRESENCE_TYPE, FridgeItem.Presence.HAVE.name)
            })
        }
        return builder.build()
    }
}
