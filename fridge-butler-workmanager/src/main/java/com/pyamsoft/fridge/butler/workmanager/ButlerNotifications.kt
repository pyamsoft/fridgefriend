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

package com.pyamsoft.fridge.butler.workmanager

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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import timber.log.Timber

internal object ButlerNotifications {

  @CheckResult
  private fun notificationManager(context: Context): NotificationManagerCompat {
    return NotificationManagerCompat.from(context)
  }

  @CheckResult
  private fun notificationId(
    entry: FridgeEntry,
    channelId: String
  ): Int {
    return "${entry.id()}_$channelId".hashCode()
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
        NotificationChannel(channelId, channelTitle, NotificationManager.IMPORTANCE_DEFAULT).apply {
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
  internal inline fun notify(
    context: Context,
    channelId: String,
    channelTitle: String,
    channelDescription: String,
    entry: FridgeEntry,
    items: List<FridgeItem>,
    createNotification: (builder: NotificationCompat.Builder) -> Notification
  ) {
    require(items.isNotEmpty())
    require(channelId.isNotBlank())
    require(channelTitle.isNotBlank())
    require(channelDescription.isNotBlank())

    guaranteeNotificationChannelExists(context, channelId, channelTitle, channelDescription)

    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_get_app_24dp)
        .setAutoCancel(false)
        .setOngoing(false)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setNumber(items.size)
        .setColor(Color.RED)

    val notification = createNotification(builder)

    Timber.d("Fire notification for entry: ${entry.id()}")
    notificationManager(context).apply {
      cancel(this, entry, channelId)
      notify(entry.id(), notificationId(entry, channelId), notification)
    }
  }

  @JvmStatic
  private fun cancel(
    manager: NotificationManagerCompat,
    entry: FridgeEntry,
    channelId: String
  ) {
    Timber.w("Cancel notification for entry: ${entry.id()}")
    manager.cancel(entry.id(), notificationId(entry, channelId))
  }

}
