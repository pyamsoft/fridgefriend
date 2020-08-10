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

package com.pyamsoft.fridge.butler.notification

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyId
import com.pyamsoft.pydroid.notify.toNotifyId
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
internal class NotificationHandlerImpl @Inject internal constructor(
    private val idMap: NotificationIdMap,
    private val notifier: Notifier
) : NotificationHandler {

    override fun notifyNeeded(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        show(
            id = idMap.getNotificationId(NotificationType.NEEDED) { entry.id().id },
            channelInfo = NotificationChannelInfo.NEEDED,
            notification = NeededItemNotifyData(
                entry = entry,
                items = items
            )
        ).also { Timber.d("Showing needed notification: $it") }
        return true
    }

    override fun notifyExpiring(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        show(
            id = idMap.getNotificationId(NotificationType.EXPIRING) { entry.id().id },
            channelInfo = NotificationChannelInfo.EXPIRING,
            notification = ExpiringItemNotifyData(
                entry = entry,
                items = items
            )
        ).also { Timber.d("Showing expiring soon notification: $it") }
        return true
    }

    override fun notifyExpired(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
        show(
            id = idMap.getNotificationId(NotificationType.EXPIRED) { entry.id().id },
            channelInfo = NotificationChannelInfo.EXPIRED,
            notification = ExpiredItemNotifyData(
                entry = entry,
                items = items
            )
        ).also { Timber.d("Showing expired notification: $it") }
        return true
    }

    private fun cancelAllNeededNotifications() {
        idMap.getNotifications(NotificationType.NEEDED)
            .values
            .forEach { cancel(it) }
    }

    @CheckResult
    private fun notifyNearby(
        nearbyId: Long,
        nearbyName: String,
        items: List<FridgeItem>
    ): Boolean {
        // Plain needed notifications are overruled by location aware Nearby notifications
        cancelAllNeededNotifications()

        show(
            id = idMap.getNotificationId(NotificationType.NEARBY) { nearbyId.toString() },
            channelInfo = NotificationChannelInfo.NEARBY,
            notification = NearbyItemNotifyData(
                name = nearbyName,
                items = items
            )
        ).also { Timber.d("Showing nearby notification: $it") }
        return true
    }

    override fun notifyNearby(zone: NearbyZone, items: List<FridgeItem>): Boolean {
        return notifyNearby(zone.id().id, zone.name(), items)
    }

    override fun notifyNearby(store: NearbyStore, items: List<FridgeItem>): Boolean {
        return notifyNearby(store.id().id, store.name(), items)
    }

    override fun notifyNightly(): Boolean {
        show(
            id = NIGHTLY_NOTIFICATION_ID,
            channelInfo = NotificationChannelInfo.NIGHTLY,
            notification = NightlyNotifyData
        ).also { Timber.d("Showing nightly notification: $it") }
        return true
    }

    @CheckResult
    private fun show(
        id: NotifyId,
        channelInfo: NotificationChannelInfo,
        notification: NotifyData
    ): NotifyId {
        cancelNotification(id)
        return notifier.show(
            id,
            NotifyChannelInfo(
                id = channelInfo.id,
                title = channelInfo.title,
                description = channelInfo.description
            ),
            notification
        )
    }

    private fun cancelNotification(id: NotifyId) {
        notifier.cancel(id)
    }

    override fun cancel(notificationId: NotifyId) {
        Timber.w("Cancel notification: $notificationId")
        cancelNotification(notificationId)
    }

    companion object {

        private val NIGHTLY_NOTIFICATION_ID = 42069.toNotifyId()
    }
}
