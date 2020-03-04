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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pyamsoft.fridge.db.item.FridgeItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NotificationHandlerImpl @Inject internal constructor(
    private val context: Context,
    private val activityClass: Class<out Activity>
) : NotificationHandler {

    override fun contentIntent(notificationId: Int, presence: FridgeItem.Presence): PendingIntent {
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
}
