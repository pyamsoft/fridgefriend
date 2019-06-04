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

package com.pyamsoft.fridge.butler.workmanager

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.cleanMidnight
import com.pyamsoft.fridge.db.daysLaterMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.ui.Injector
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.TimeUnit

internal class ExpirationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : RxWorker(context, params) {

  private var fridgeEntryQueryDao: FridgeEntryQueryDao? = null
  private var fridgeItemQueryDao: FridgeItemQueryDao? = null
  private var butler: Butler? = null

  private fun inject() {
    fridgeEntryQueryDao = Injector.obtain(applicationContext)
    fridgeItemQueryDao = Injector.obtain(applicationContext)
    butler = Injector.obtain(applicationContext)
  }

  private fun teardown() {
    fridgeEntryQueryDao = null
    fridgeItemQueryDao = null
    butler = null
  }

  private fun reschedule(required: Boolean) {
    val time = 3L
    val unit = TimeUnit.HOURS
    if (required) {
      requireNotNull(butler).remindExpiration(time, unit)
    } else {
      butler?.remindExpiration(time, unit)
    }
  }

  override fun onStopped() {
    super.onStopped()

    Timber.i("ExpirationWorker stopped")
    reschedule(required = false)
    teardown()
  }

  private fun notifyForEntry(
    today: Calendar,
    later: Calendar,
    entry: FridgeEntry,
    items: List<FridgeItem>
  ) {
    val needItems = arrayListOf<FridgeItem>()
    val expiringItems = arrayListOf<FridgeItem>()
    val expiredItems = arrayListOf<FridgeItem>()
    val unknownExpirationItems = arrayListOf<FridgeItem>()

    for (item in items.filterNot { it.isArchived() }) {
      if (item.presence() == NEED) {
        Timber.d("${entry.id()} needs item: $item")
        needItems.add(item)
      } else {
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


    if (needItems.isNotEmpty()) {
      ExpirationNotifications.notifyNeeded(applicationContext, entry, needItems)
    }

    if (expiringItems.isNotEmpty()) {
      ExpirationNotifications.notifyExpiring(applicationContext, entry, expiringItems)
    }

    if (expiredItems.isNotEmpty()) {
      ExpirationNotifications.notifyExpired(applicationContext, entry, expiredItems)
    }

    if (unknownExpirationItems.isNotEmpty()) {
      Timber.w("Butler cannot handle unknowns: $unknownExpirationItems")
    }
  }

  override fun createWork(): Single<Result> {
    inject()

    return Single.defer {

      val today = Calendar.getInstance()
          .cleanMidnight()
      val later = Calendar.getInstance()
          .daysLaterMidnight(2)

      return@defer requireNotNull(fridgeEntryQueryDao).queryAll(true)
          .flatMapObservable { Observable.fromIterable(it) }
          .flatMapSingle { entry ->
            return@flatMapSingle requireNotNull(fridgeItemQueryDao).queryAll(true, entry.id())
                .doOnSuccess { items -> notifyForEntry(today, later, entry, items) }
                .map { entry }
          }
          .toList()
          .map { success(it) }
          .onErrorReturn { fail(it) }
    }
        .doAfterTerminate { teardown() }
  }

  @CheckResult
  private fun success(entries: List<FridgeEntry>): Result {
    Timber.d("Butler notified for entries: $entries")
    reschedule(required = true)
    return Result.success()
  }

  @CheckResult
  private fun fail(throwable: Throwable): Result {
    Timber.e(throwable, "Butler failed to notify")
    reschedule(required = true)
    return Result.failure()
  }
}
