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
import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.app.NotificationCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.italic
import com.pyamsoft.fridge.butler.notification.NotificationChannelInfo
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.getExpiredMessage
import com.pyamsoft.fridge.db.item.getExpiringSoonMessage
import com.pyamsoft.pydroid.notify.NotifyData

internal abstract class ItemNotifyDispatcher<T : NotifyData> protected constructor(
    context: Context,
    activityClass: Class<out Activity>,
    channel: NotificationChannelInfo
) : BaseNotifyDispatcher<T>(context, activityClass, channel) {

    @CheckResult
    protected fun createBigTextStyle(
        topLine: CharSequence?,
        items: List<FridgeItem>,
        isExpired: Boolean,
        isExpiringSoon: Boolean
    ): NotificationCompat.Style {
        require(!(isExpired && isExpiringSoon)) { "Items cannot be expired and expiring soon!" }

        val now = today()

        return NotificationCompat.BigTextStyle().bigText(
            buildSpannedString {
                topLine?.let { line ->
                    appendln(line)
                    appendln("-".repeat(40))
                    appendln()
                }
                items
                    .forEach { item ->

                        bold {
                            append(
                                when {
                                    isExpiringSoon -> FridgeItem.MARK_EXPIRING_SOON
                                    isExpired -> FridgeItem.MARK_EXPIRED
                                    else -> FridgeItem.MARK_FRESH
                                }
                            )
                        }
                        append("   ")
                        italic { append(item.name()) }
                        append("  ")

                        if (isExpiringSoon) {
                            append(" ${item.getExpiringSoonMessage(now)}")
                        }
                        if (isExpired) {
                            append(" ${item.getExpiredMessage(now)}")
                        }
                        appendln()
                    }
            })
    }
}
