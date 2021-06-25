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
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.preference.NightlyPreferences
import com.pyamsoft.fridge.preference.NotificationPreferences
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
internal class NightlyRunner
@Inject
internal constructor(
    private val orderFactory: OrderFactory,
    private val butler: Butler,
    private val fridgeItemQueryDao: FridgeItemQueryDao,
    private val nightlyPreferences: NightlyPreferences,
    handler: NotificationHandler,
    notificationPreferences: NotificationPreferences,
) : BaseRunner<EmptyParameters>(handler, notificationPreferences) {

  private fun CoroutineScope.handleReschedule() =
      launch(context = Dispatchers.Default) {
        val order = orderFactory.nightlyOrder()
        Timber.d("Rescheduling order: $order")
        butler.placeOrder(order)
      }

  private suspend fun notifyNightly(
      items: List<FridgeItem>,
      now: Calendar,
  ) {
    if (items.isNotEmpty()) {
      val lastTime = nightlyPreferences.getLastNotificationTimeNightly()
      if (now.isAllowedToNotify(false, lastTime) && isAtleastTime(now)) {
        Timber.d("Notify user about items nightly fridge cleanup")
        if (notification { notifyNightly() }) {
          nightlyPreferences.markNotificationNightly(now)
        }
      }
    }
  }

  override suspend fun performWork(params: EmptyParameters) = coroutineScope {
    val now = today()
    val items = fridgeItemQueryDao.query(false)
    notifyNightly(items.filter { it.presence() == HAVE }, now).also { handleReschedule() }
  }

  @CheckResult
  private fun isAtleastTime(now: Calendar): Boolean {
    val hour = now.get(Calendar.HOUR_OF_DAY)
    return hour >= EVENING_HOUR
  }

  companion object {
    // 8PM
    private const val EVENING_HOUR = 8 + 12
  }
}
