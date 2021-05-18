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

package com.pyamsoft.fridge.butler.notification

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerInternalApi
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.notify.Notifier
import com.pyamsoft.pydroid.notify.NotifyChannelInfo
import com.pyamsoft.pydroid.notify.NotifyData
import com.pyamsoft.pydroid.notify.NotifyId
import com.pyamsoft.pydroid.notify.toNotifyId
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
internal class NotificationHandlerImpl
@Inject
internal constructor(
    @param:ButlerInternalApi private val notifier: Notifier,
    private val idMap: NotificationIdMap
) : NotificationHandler {

  override fun notifyNeeded(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
    show(
        id = idMap.getNotificationId(NotificationType.NEEDED) { entry.id().id },
        channelInfo = NotificationChannelInfo.NEEDED,
        notification = NeededItemNotifyData(entry = entry, items = items))
        .also { Timber.d("Showing needed notification: $it") }
    return true
  }

  override fun notifyExpiring(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
    show(
        id = idMap.getNotificationId(NotificationType.EXPIRING) { entry.id().id },
        channelInfo = NotificationChannelInfo.EXPIRING,
        notification = ExpiringItemNotifyData(entry = entry, items = items))
        .also { Timber.d("Showing expiring soon notification: $it") }
    return true
  }

  override fun notifyExpired(entry: FridgeEntry, items: List<FridgeItem>): Boolean {
    show(
        id = idMap.getNotificationId(NotificationType.EXPIRED) { entry.id().id },
        channelInfo = NotificationChannelInfo.EXPIRED,
        notification = ExpiredItemNotifyData(entry = entry, items = items))
        .also { Timber.d("Showing expired notification: $it") }
    return true
  }

  override fun notifyNightly(): Boolean {
    show(
        id = NIGHTLY_NOTIFICATION_ID,
        channelInfo = NotificationChannelInfo.NIGHTLY,
        notification = NightlyNotifyData)
        .also { Timber.d("Showing nightly notification: $it") }
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
            id = channelInfo.id, title = channelInfo.title, description = channelInfo.description),
        notification)
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
