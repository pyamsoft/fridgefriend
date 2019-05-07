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

  private val RANDOM_ID_SET = (Int.MIN_VALUE..Int.MAX_VALUE)
  private const val NEEDED_CHANNEL_ID = "fridge_need_reminders_channel_v1"
  private const val EXPIRING_CHANNEL_ID = "fridge_expiring_reminders_channel_v1"
  private const val EXPIRED_CHANNEL_ID = "fridge_expiration_reminders_channel_v1"

  private val lock = Any()
  private val knownNotifications = LinkedHashMap<String, Set<Int>>()

  @CheckResult
  private fun notificationManager(context: Context): NotificationManagerCompat {
    return NotificationManagerCompat.from(context)
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
          enableVibration(false)
          setSound(null, null)
        }

      Timber.d("Create notification channel and group with id: $channelId")
      requireNotNull(context.getSystemService<NotificationManager>()).let { manager ->
        manager.createNotificationChannelGroup(notificationGroup)
        manager.createNotificationChannel(notificationChannel)
      }
    }
  }

  @CheckResult
  private fun getExtraItems(items: List<FridgeItem>): String {
    return when {
      items.size == 1 -> ""
      items.size == 2 -> "and ${items[1].name()}"
      else -> "and ${items.size - 1} other items"
    }
  }

  @CheckResult
  private fun generateNotificationId(): Int {
    synchronized(lock) {
      var random = RANDOM_ID_SET.random()
      while (true) {
        for (listOfKnownIds in knownNotifications.values) {
          if (random in listOfKnownIds) {
            random = RANDOM_ID_SET.random()
            continue
          }
        }

        // We are random enough
        break
      }

      return random
    }
  }

  @JvmStatic
  private fun notify(
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

    val id = generateNotificationId()
    Timber.d("Fire notification for entry: ${entry.id()} $id")
    notificationManager(context).notify(entry.id(), id, notification)

    synchronized(lock) {
      if (knownNotifications.containsKey(entry.id())) {
        knownNotifications[entry.id()] = setOf(id) + knownNotifications.getValue(entry.id())
      } else {
        knownNotifications[entry.id()] = setOf(id)
      }
    }
  }

  @JvmStatic
  fun notifyNeeded(context: Context, entry: FridgeEntry, items: List<FridgeItem>) {
    notify(
      context,
      NEEDED_CHANNEL_ID,
      "Purchase Reminders",
      "Reminders for items you still need to purchase",
      entry,
      items
    ) { builder ->
      return@notify builder
        .setContentTitle("Purchase reminder for ${entry.name()}")
        .setContentText("You still need ${items.first().name()} ${getExtraItems(items)}")
        .build()
    }
  }

  @JvmStatic
  fun notifyExpiring(context: Context, entry: FridgeEntry, items: List<FridgeItem>) {
    notify(
      context,
      EXPIRING_CHANNEL_ID,
      "Expiring Reminders",
      "Reminders for items that are going to expire soon",
      entry,
      items
    ) { builder ->
      val extra = "${getExtraItems(items)} ${if (items.size == 1) "is" else "are"} about to expire."
      return@notify builder
        .setContentTitle("Expiration reminder for ${entry.name()}")
        .setContentText("${items.first().name()} $extra")
        .build()
    }
  }

  @JvmStatic
  fun notifyExpired(context: Context, entry: FridgeEntry, items: List<FridgeItem>) {
    notify(
      context,
      EXPIRED_CHANNEL_ID,
      "Expired Reminders",
      "Reminders for items that have expired",
      entry,
      items
    ) { builder ->
      return@notify builder
        .setContentTitle("Expired warning for ${entry.name()}")
        .setContentText("${items.first().name()} ${getExtraItems(items)} ${if (items.size == 1) "has" else "have"} passed expiration!")
        .build()
    }
  }

  @JvmStatic
  fun cancel(context: Context, entry: FridgeEntry) {
    synchronized(lock) {
      var burned = false

      for (key in knownNotifications.keys) {
        if (key == entry.id()) {
          burned = true
          val notificationManager = notificationManager(context)
          for (notificationId in knownNotifications.getValue(key)) {
            Timber.w("Cancel notification for entry: ${entry.id()} $notificationId")
            notificationManager.cancel(entry.id(), notificationId)
          }
          break
        }
      }

      if (burned) {
        knownNotifications.remove(entry.id())
      }
    }
  }

}
