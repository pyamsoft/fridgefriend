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
import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import com.pyamsoft.fridge.core.Notifications
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.pydroid.ui.Injector
import timber.log.Timber

object ButlerNotifications {

  @CheckResult
  private fun notificationId(
    id: String,
    channelId: String
  ): Int {
    return "${id}_$channelId".hashCode()
  }

  @JvmStatic
  fun notify(
    entry: FridgeEntry,
    context: Context,
    channelId: String,
    channelTitle: String,
    channelDescription: String,
    createNotification: (builder: NotificationCompat.Builder) -> Notification
  ) {
    if (Injector.obtain<ForegroundState>(context.applicationContext).isForeground) {
      Timber.w("Do not send notification while in foreground: ${entry.id()}")
      return
    }

    Notifications.notify(
        context,
        notificationId(entry.id(), channelId),
        entry.id(),
        R.drawable.ic_get_app_24dp,
        channelId,
        channelTitle,
        channelDescription,
        createNotification
    )
  }
}
