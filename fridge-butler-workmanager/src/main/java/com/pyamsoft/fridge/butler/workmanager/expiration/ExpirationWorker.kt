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
import com.pyamsoft.fridge.butler.workmanager.BaseWorker
import com.pyamsoft.fridge.db.cleanMidnight
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.ui.Injector
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit.HOURS

internal class ExpirationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : BaseWorker(context, params) {

  private var fridgeEntryQueryDao: FridgeEntryQueryDao? = null
  private var fridgeItemQueryDao: FridgeItemQueryDao? = null

  override fun onInject() {
    fridgeEntryQueryDao = Injector.obtain(applicationContext)
    fridgeItemQueryDao = Injector.obtain(applicationContext)
  }

  override fun onTeardown() {
    fridgeEntryQueryDao = null
    fridgeItemQueryDao = null
  }

  override fun reschedule(butler: Butler) {
    butler.cancelExpirationReminder()
    butler.remindExpiration(3L, HOURS)
  }

  private fun notifyForEntry(
    today: Calendar,
    later: Calendar,
    entry: FridgeEntry,
    items: List<FridgeItem>
  ) {
    enforcer.assertNotOnMainThread()

    val expiringItems = arrayListOf<FridgeItem>()
    val expiredItems = arrayListOf<FridgeItem>()
    val unknownExpirationItems = arrayListOf<FridgeItem>()

    for (item in items.filterNot { it.isArchived() }) {
      if (item.presence() == HAVE) {
        val expirationTime = item.expireTime()
        if (expirationTime != FridgeItem.EMPTY_EXPIRE_TIME) {

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
      ExpirationNotifications.notifyExpiring(
          applicationContext, entry, expiringItems
      )
    }

    if (expiredItems.isNotEmpty()) {
      ExpirationNotifications.notifyExpired(
          applicationContext, entry, expiredItems
      )
    }

    if (unknownExpirationItems.isNotEmpty()) {
      Timber.w("Butler cannot handle unknowns: $unknownExpirationItems")
    }
  }

  override fun doWork(): Single<*> {
    enforcer.assertNotOnMainThread()
    val today = Calendar.getInstance()
        .cleanMidnight()
    val later = Calendar.getInstance()
        .daysLaterMidnight(2)

    return requireNotNull(fridgeEntryQueryDao).queryAll(true)
        .flatMapObservable {
          enforcer.assertNotOnMainThread()
          return@flatMapObservable Observable.fromIterable(it)
              .subscribeOn(backgroundScheduler)
              .observeOn(backgroundScheduler)
        }
        .flatMapSingle { entry ->
          enforcer.assertNotOnMainThread()
          return@flatMapSingle requireNotNull(fridgeItemQueryDao).queryAll(true, entry.id())
              .subscribeOn(backgroundScheduler)
              .observeOn(backgroundScheduler)
              .doOnSuccess { items -> notifyForEntry(today, later, entry, items) }
              .map { entry }
        }
        .toList()
  }
}
