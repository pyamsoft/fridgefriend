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

package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.work.Order
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.fridge.Fridge
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.cleanMidnight
import com.pyamsoft.fridge.db.item.daysLaterMidnight
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.db.item.isExpired
import com.pyamsoft.fridge.db.item.isExpiringSoon
import com.pyamsoft.fridge.preference.DetailPreferences
import com.pyamsoft.fridge.preference.ExpiredPreferences
import com.pyamsoft.fridge.preference.ExpiringPreferences
import com.pyamsoft.fridge.preference.NeededPreferences
import com.pyamsoft.fridge.preference.NotificationPreferences
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

@Singleton
internal class ItemRunner
@Inject
internal constructor(
    private val butler: Butler,
    private val fridge: Fridge,
    private val detailPreferences: DetailPreferences,
    private val neededPreferences: NeededPreferences,
    private val expiringPreferences: ExpiringPreferences,
    private val expiredPreferences: ExpiredPreferences,
    handler: NotificationHandler,
    notificationPreferences: NotificationPreferences,
) : BaseRunner<ItemParameters>(handler, notificationPreferences) {

  override suspend fun onReschedule(order: Order) {
    Timber.d("Rescheduling order: $order")
    butler.scheduleOrder(order)
  }

  @CheckResult
  private suspend fun notifyNeeded(
      items: List<FridgeItem>,
      entry: FridgeEntry,
      now: Calendar,
      params: ItemParameters,
  ): Boolean {
    if (items.isNotEmpty()) {
      val lastTime = neededPreferences.getLastNotificationTimeNeeded()
      if (now.isAllowedToNotify(params.forceNotifyNeeded, lastTime)) {
        Timber.d("Notify user about items still needed")
        return notification { notifyNeeded(entry, items) }
      }
    }

    return false
  }

  @CheckResult
  private suspend fun notifyExpiringSoon(
      items: List<FridgeItem>,
      entry: FridgeEntry,
      now: Calendar,
      params: ItemParameters,
  ): Boolean {
    if (items.isNotEmpty()) {
      val lastTime = expiringPreferences.getLastNotificationTimeExpiringSoon()
      if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
        Timber.d("Notify user about items expiring soon")
        return notification { notifyExpiring(entry, items) }
      }
    }

    return false
  }

  @CheckResult
  private suspend fun notifyExpired(
      items: List<FridgeItem>,
      entry: FridgeEntry,
      now: Calendar,
      params: ItemParameters,
  ): Boolean {
    if (items.isNotEmpty()) {
      val lastTime = expiredPreferences.getLastNotificationTimeExpired()
      if (now.isAllowedToNotify(params.forceNotifyExpiring, lastTime)) {
        Timber.d("Notify user about items expired")
        return notification { notifyExpired(entry, items) }
      }
    }

    return false
  }

  @CheckResult
  private suspend fun notifyForEntry(
      params: ItemParameters,
      now: Calendar,
      current: Calendar,
      later: Calendar,
      isSameDayExpired: Boolean,
      entry: FridgeEntry,
      items: List<FridgeItem>,
  ): NotifyResults {
    val neededItems = mutableListOf<FridgeItem>()
    val expiringItems = mutableListOf<FridgeItem>()
    val expiredItems = mutableListOf<FridgeItem>()

    items.filterNot { it.isArchived() }.forEach { item ->
      when (item.presence()) {
        HAVE -> {
          if (item.isExpired(current, isSameDayExpired)) {
            Timber.w("${item.name()} expired!")
            expiredItems.add(item)
          } else {
            if (item.isExpiringSoon(current, later, isSameDayExpired)) {
              Timber.w("${item.name()} is expiring soon!")
              expiringItems.add(item)
            }
          }
        }
        NEED -> {
          Timber.w("Still need ${item.name()}")
          neededItems.add(item)
        }
      }
    }

    val needed = notifyNeeded(neededItems, entry, now, params)
    val expiring = notifyExpiringSoon(expiringItems, entry, now, params)
    val expired = notifyExpired(expiredItems, entry, now, params)
    return NotifyResults(entry.id(), needed, expiring, expired, nearby = false)
  }

  override suspend fun performWork(params: ItemParameters) = coroutineScope {
    val now = today()
    val today = today().cleanMidnight()
    val later = today().daysLaterMidnight(detailPreferences.getExpiringSoonRange())
    val isSameDayExpired = detailPreferences.isSameDayExpired()

    fridge
        .forAllItemsInEachEntry(true) { entry, items ->
          notifyForEntry(params, now, today, later, isSameDayExpired, entry, items)
        }
        .let { results ->
          results.firstOrNull { it.needed }?.let { neededPreferences.markNotificationNeeded(now) }

          results.firstOrNull { it.expiring }?.let {
            expiringPreferences.markNotificationExpiringSoon(now)
          }

          results.firstOrNull { it.expired }?.let {
            expiredPreferences.markNotificationExpired(now)
          }

          // Unit
          return@coroutineScope
        }
  }
}
