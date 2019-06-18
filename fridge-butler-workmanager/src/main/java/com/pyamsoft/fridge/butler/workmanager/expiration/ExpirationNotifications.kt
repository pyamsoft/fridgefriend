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

package com.pyamsoft.fridge.butler.workmanager.expiration

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.ButlerNotifications
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem

internal object ExpirationNotifications {

  private const val EXPIRING_CHANNEL_ID = "fridge_expiring_reminders_channel_v1"
  private const val EXPIRED_CHANNEL_ID = "fridge_expiration_reminders_channel_v1"

  @CheckResult
  private fun getExtraItems(items: List<FridgeItem>): String {
    return when {
      items.size == 1 -> ""
      items.size == 2 -> "and '${items[1].name()}'"
      else -> "and ${items.size - 1} other items"
    }
  }

  @JvmStatic
  fun notifyExpiring(
    context: Context,
    entry: FridgeEntry,
    items: List<FridgeItem>
  ) {
    ButlerNotifications.notify(
        entry,
        context,
        EXPIRING_CHANNEL_ID,
        "Expiring Reminders",
        "Reminders for items that are going to expire soon"
    ) { builder ->
      val extra =
        "${getExtraItems(
            items
        )} ${if (items.size == 1) "is" else "are"} about to expire."
      return@notify builder
          .setContentTitle("Expiration reminder for '${entry.name()}'")
          .setContentText("'${items.first().name()}' $extra")
          .build()
    }
  }

  @JvmStatic
  fun notifyExpired(
    context: Context,
    entry: FridgeEntry,
    items: List<FridgeItem>
  ) {
    ButlerNotifications.notify(
        entry,
        context,
        EXPIRED_CHANNEL_ID,
        "Expired Reminders",
        "Reminders for items that have expired"
    ) { builder ->
      return@notify builder
          .setContentTitle("Expired warning for '${entry.name()}'")
          .setContentText(
              "'${items.first().name()}' ${getExtraItems(
                  items
              )} ${if (items.size == 1) "has" else "have"} passed expiration!"
          )
          .build()
    }
  }

}
