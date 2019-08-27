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
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.FridgeWorker
import com.pyamsoft.fridge.db.cleanMidnight
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.isArchived
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS

internal class ExpirationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : FridgeWorker(context, params) {

  override fun reschedule(butler: Butler) {
    butler.remindExpiration(3L, HOURS)
  }

  private fun notifyForEntry(
    today: Calendar,
    later: Calendar,
    entry: FridgeEntry,
    items: List<FridgeItem>
  ) {
    val expiringItems = arrayListOf<FridgeItem>()
    val expiredItems = arrayListOf<FridgeItem>()
    val unknownExpirationItems = arrayListOf<FridgeItem>()

    for (item in items.filterNot { it.isArchived() }) {
      if (item.presence() == HAVE) {
        val expirationTime = item.expireTime()
        if (expirationTime != null) {

          if (item.isExpired(today)) {
            Timber.w("${entry.id()} expired! $item")
            expiredItems.add(item)
          } else {
            if (item.isExpiringSoon(today, later)) {
              Timber.w("${entry.id()} is expiring soon! $item")
              expiringItems.add(item)
            }
          }
        } else {
          unknownExpirationItems.add(item)
        }
      }
    }

    if (expiringItems.isNotEmpty()) {
      notification { handler, foregroundState ->
        ExpirationNotifications.notifyExpiring(
            handler, foregroundState, applicationContext, entry, expiringItems
        )
      }
    }

    if (expiredItems.isNotEmpty()) {
      notification { handler, foregroundState ->
        ExpirationNotifications.notifyExpired(
            handler, foregroundState, applicationContext, entry, expiredItems
        )
      }
    }

    if (unknownExpirationItems.isNotEmpty()) {
      Timber.w("Butler cannot handle unknowns: $unknownExpirationItems")
    }
  }

  override suspend fun performWork() {
    return coroutineScope {
      val today = Calendar.getInstance()
          .cleanMidnight()
      val later = Calendar.getInstance()
          .daysLaterMidnight(2)
      withFridgeData { entry, items ->
        notifyForEntry(today, later, entry, items)
      }
    }
  }
}
