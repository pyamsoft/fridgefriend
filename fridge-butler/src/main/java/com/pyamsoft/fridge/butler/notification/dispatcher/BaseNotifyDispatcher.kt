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
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.pyamsoft.fridge.butler.R
import com.pyamsoft.fridge.butler.notification.NotificationChannelInfo
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyDispatcher
import com.pyamsoft.pydroid.notify.NotifyId
import com.pyamsoft.pydroid.notify.asNotifyId
import timber.log.Timber

internal abstract class BaseNotifyDispatcher<T : NotifyData> protected constructor(
    private val context: Context,
    private val activityClass: Class<out Activity>,
    private val channel: NotificationChannelInfo
) : NotifyDispatcher<T> {

    private val primaryColor by lazy {
        // NOTE(Peter): May not be accurate if we are not using Activity context.
        ContextCompat.getColor(context.applicationContext, R.color.colorPrimary)
    }

    private val channelCreator by lazy {
        requireNotNull(context.applicationContext.getSystemService<NotificationManager>())
    }

    private fun guaranteeNotificationChannelExists() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationGroup = NotificationChannelGroup(channel.id, channel.title)
            val notificationChannel = NotificationChannel(
                channel.id,
                channel.title,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                group = notificationGroup.id
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                description = channel.description
                enableLights(false)
                enableVibration(true)
            }

            Timber.d("Create notification channel and group: ${channel.id} ${channel.title}")
            channelCreator.apply {
                createNotificationChannelGroup(notificationGroup)
                createNotificationChannel(notificationChannel)
            }
        }
    }

    final override fun build(
        id: NotifyId,
        notification: T
    ): Notification {
        guaranteeNotificationChannelExists()

        val builder = NotificationCompat.Builder(context, channel.id)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(primaryColor)
        return onBuildNotification(id, notification, builder)
    }

    @CheckResult
    protected abstract fun onBuildNotification(
        id: NotifyId,
        notification: T,
        builder: NotificationCompat.Builder
    ): Notification

    @CheckResult
    protected fun createContentIntent(
        notificationId: NotifyId,
        presence: FridgeItem.Presence
    ): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            putExtra(FridgeItem.Presence.KEY, presence.name)
            putExtra(NotificationHandler.NOTIFICATION_ID_KEY, notificationId.id)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        return PendingIntent.getActivity(
            context,
            notificationId.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @CheckResult
    protected fun <S : Any> MutableMap<S, NotifyId>.getNotificationId(key: S): NotifyId {
        return this.getOrPut(key) {
            val rawId = key.hashCode()
            return@getOrPut rawId.asNotifyId()
        }
    }
}
