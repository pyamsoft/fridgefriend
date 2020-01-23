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

package com.pyamsoft.fridge.butler

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.pyamsoft.fridge.db.item.FridgeItem
import timber.log.Timber

object Notifications {

    @CheckResult
    private fun notificationManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context.applicationContext)
    }

    private fun guaranteeNotificationChannelExists(
        context: Context,
        channelId: String,
        channelTitle: String,
        channelDescription: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationGroup = NotificationChannelGroup(channelId, channelTitle)
            val notificationChannel =
                NotificationChannel(
                    channelId,
                    channelTitle,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    group = notificationGroup.id
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    description = channelDescription
                    enableLights(false)
                    enableVibration(true)
                }

            Timber.d("Create notification channel and group with id: $channelId")
            requireNotNull(context.getSystemService<NotificationManager>()).let { manager ->
                manager.createNotificationChannelGroup(notificationGroup)
                manager.createNotificationChannel(notificationChannel)
            }
        }
    }

    @JvmStatic
    fun notify(
        notificationId: Int,
        handler: NotificationHandler,
        context: Context,
        icon: Int,
        channelId: String,
        channelTitle: String,
        channelDescription: String,
        presence: FridgeItem.Presence,
        createNotification: (builder: NotificationCompat.Builder) -> Notification
    ) {
        require(notificationId > 0)
        require(channelId.isNotBlank())
        require(channelTitle.isNotBlank())
        require(channelDescription.isNotBlank())

        guaranteeNotificationChannelExists(
            context, channelId, channelTitle, channelDescription
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.RED)
            .setContentIntent(handler.contentIntent(notificationId, presence))

        val notification = createNotification(builder)
        notificationManager(context)
            .apply {
                cancel(this, notificationId)
                notify(notificationId, notification)
            }
    }

    @JvmStatic
    private fun cancel(
        manager: NotificationManagerCompat,
        notificationId: Int
    ) {
        Timber.w("Cancel notification: $notificationId")
        manager.cancel(notificationId)
    }

    fun cancel(context: Context, notificationId: Int) {
        cancel(notificationManager(context), notificationId)
    }
}
