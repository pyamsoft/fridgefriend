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

package com.pyamsoft.fridge.butler.notification.dispatcher

import android.app.Activity
import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import com.pyamsoft.fridge.butler.notification.NearbyItemNotifyData
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NearbyItemNotifyDispatcher @Inject internal constructor(
    context: Context,
    activityClass: Class<out Activity>
) : ItemNotifyDispatcher<NearbyItemNotifyData>(
    context,
    activityClass = activityClass
) {

    override fun canShow(notification: NotifyData): Boolean {
        return notification is NearbyItemNotifyData
    }

    override fun onBuildNotification(
        id: NotifyId,
        notification: NearbyItemNotifyData,
        builder: NotificationCompat.Builder
    ): Notification {
        builder.apply {
            setSmallIcon(R.drawable.ic_shopping_cart_24dp)
            setContentTitle(buildSpannedString {
                bold { append("Nearby reminder") }
                append(" for ")
                bold { append(notification.name) }
            })
            setContentText(buildSpannedString {
                append("You still ")
                bold { append("need to shop") }
                append(" for ")
                bold { append("${notification.items.size}") }
                append(" items")
            })

            setStyle(
                createBigTextStyle(
                    null,
                    notification.items,
                    isExpired = false,
                    isExpiringSoon = false
                )
            )

            setContentIntent(createContentIntent(id) {
                putExtra(NotificationHandler.KEY_NEARBY_ID, notification.id)
            })
        }
        return builder.build()
    }
}
