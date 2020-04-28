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

package com.pyamsoft.fridge.butler

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
internal class NotificationHandlerImpl @Inject internal constructor(
    private val context: Context,
    private val activityClass: Class<out Activity>
) : NotificationHandler {

    private val manager by lazy { requireNotNull(context.getSystemService<NotificationManager>()) }

    private val nearbyNotificationIdMap by lazy { mutableMapOf<Long, Int>() }
    private val needNotificationIdMap by lazy { mutableMapOf<FridgeEntry.Id, Int>() }
    private val expiringNotificationIdMap by lazy { mutableMapOf<FridgeEntry.Id, Int>() }
    private val expiredNotificationIdMap by lazy { mutableMapOf<FridgeEntry.Id, Int>() }

    @CheckResult
    private fun contentIntent(notificationId: Int, presence: FridgeItem.Presence): PendingIntent {
        val intent = Intent(context, activityClass).apply {
            putExtra(FridgeItem.Presence.KEY, presence.name)
            putExtra(NotificationHandler.NOTIFICATION_ID_KEY, notificationId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        return PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    override fun notifyNeeded(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        val id = needNotificationIdMap.getNotificationId(entry.id())
        return notify(
            id,
            R.drawable.ic_get_app_24dp,
            NEEDED_CHANNEL_ID,
            "Needed Reminders",
            "Reminders for items that you still need.",
            contentIntent(id, FridgeItem.Presence.NEED)
        ) {
            setContentTitle("Needed reminder for ${entry.name()}")
            setContentText("You still need '${items.first().name()}' ${getExtraItems(items)}")
        }
    }

    override fun notifyExpiring(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        val id = expiringNotificationIdMap.getNotificationId(entry.id())
        return notify(
            id,
            R.drawable.ic_get_app_24dp,
            EXPIRING_CHANNEL_ID,
            "Expiring Reminders",
            "Reminders for items that are going to expire soon",
            contentIntent(id, FridgeItem.Presence.HAVE)
        ) {
            val extra =
                "${getExtraItems(items)} ${if (items.size == 1) "is" else "are"} about to expire."
            setContentTitle("Expiration reminder for '${entry.name()}'")
            setContentText("'${items.first().name()}' $extra")
        }
    }

    override fun notifyExpired(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        val id = expiredNotificationIdMap.getNotificationId(entry.id())
        return notify(
            id,
            R.drawable.ic_get_app_24dp,
            EXPIRED_CHANNEL_ID,
            "Expired Reminders",
            "Reminders for items that have expired",
            contentIntent(id, FridgeItem.Presence.HAVE)
        ) {
            setContentTitle("Expired warning for '${entry.name()}'")
            setContentText(
                "'${items.first()
                    .name()}' ${getExtraItems(items)} ${if (items.size == 1) "has" else "have"} passed expiration!"
            )
        }
    }

    override fun notifyNearby(zone: NearbyZone, items: List<FridgeItem>): Boolean {
        val id = nearbyNotificationIdMap.getNotificationId(zone.id().id)
        return notifyNearby(id, zone.name(), items)
    }

    override fun notifyNearby(store: NearbyStore, items: List<FridgeItem>): Boolean {
        val id = nearbyNotificationIdMap.getNotificationId(store.id().id)
        return notifyNearby(id, store.name(), items)
    }

    private fun cancelAllNeededNotifications() {
        val ids = needNotificationIdMap.values
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ids.parallelStream().forEach { cancel(it) }
        } else {
            ids.forEach { cancel(it) }
        }
    }

    @CheckResult
    private fun notifyNearby(
        notificationId: Int,
        name: String,
        items: List<FridgeItem>
    ): Boolean {
        // Plain needed notifications are overruled by location aware Nearby notifications
        cancelAllNeededNotifications()

        return notify(
            notificationId,
            R.drawable.ic_get_app_24dp,
            NEARBY_CHANNEL_ID,
            "Nearby Reminders",
            "Reminders for items that may be at locations nearby.",
            contentIntent(notificationId, FridgeItem.Presence.NEED)
        ) {
            setContentTitle("Nearby Reminder for $name")
            setContentText("You still need '${items.first().name()}' ${getExtraItems(items)}")
        }
    }

    @CheckResult
    private fun notificationManager(context: Context): NotificationManagerCompat {
        return NotificationManagerCompat.from(context.applicationContext)
    }

    private fun guaranteeNotificationChannelExists(
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
            manager.apply {
                createNotificationChannelGroup(notificationGroup)
                createNotificationChannel(notificationChannel)
            }
        }
    }

    @CheckResult
    private fun notify(
        notificationId: Int,
        icon: Int,
        channelId: String,
        channelTitle: String,
        channelDescription: String,
        pendingIntent: PendingIntent,
        createNotification: NotificationCompat.Builder.() -> NotificationCompat.Builder
    ): Boolean {
        require(notificationId > 0)
        require(channelId.isNotBlank())
        require(channelTitle.isNotBlank())
        require(channelDescription.isNotBlank())

        guaranteeNotificationChannelExists(channelId, channelTitle, channelDescription)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setAutoCancel(false)
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(Color.RED)
            .setContentIntent(pendingIntent)

        val notification = createNotification(builder).build()
        notificationManager(context)
            .apply {
                cancel(this, notificationId)
                notify(notificationId, notification)
            }
        return true
    }

    private fun cancel(manager: NotificationManagerCompat, notificationId: Int) {
        manager.cancel(notificationId)
    }

    override fun cancel(notificationId: Int) {
        Timber.w("Cancel notification: $notificationId")
        cancel(notificationManager(context), notificationId)
    }

    companion object {

        private const val EXPIRING_CHANNEL_ID = "fridge_expiring_reminders_channel_v1"
        private const val EXPIRED_CHANNEL_ID = "fridge_expiration_reminders_channel_v1"
        private const val NEEDED_CHANNEL_ID = "fridge_needed_reminders_channel_v1"
        private const val NEARBY_CHANNEL_ID = "fridge_nearby_reminders_channel_v1"
        private val RANDOM_RANGE = (1000 until Int.MAX_VALUE)
        private val alreadyGeneratedIds by lazy { mutableSetOf<Int>() }

        @JvmStatic
        @CheckResult
        private fun <S : Any> MutableMap<S, Int>.getNotificationId(key: S): Int {
            return this.getOrPut(key) {
                var id: Int
                do {
                    id = generateNewNotificationId()
                } while (alreadyGeneratedIds.contains(id))
                alreadyGeneratedIds.add(id)
                return@getOrPut id
            }
        }

        @JvmStatic
        @CheckResult
        private fun generateNewNotificationId(): Int {
            return RANDOM_RANGE.random()
        }

        @JvmStatic
        @CheckResult
        private fun getExtraItems(items: List<FridgeItem>): String {
            return when (items.size) {
                1 -> ""
                2 -> "and '${items[1].name()}'"
                else -> "and ${items.size - 1} other items"
            }
        }
    }
}
