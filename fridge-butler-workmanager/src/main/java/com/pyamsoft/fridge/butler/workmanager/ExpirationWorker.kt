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
import com.pyamsoft.fridge.db.atMidnight
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.isExpired
import com.pyamsoft.fridge.db.isExpiringSoon
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.tomorrowMidnight
import com.pyamsoft.pydroid.ui.Injector
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.util.Calendar

internal class ExpirationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : RxWorker(context, params) {

  private var fridgeEntryQueryDao: FridgeEntryQueryDao? = null
  private var fridgeItemQueryDao: FridgeItemQueryDao? = null

  private fun inject() {
    fridgeEntryQueryDao = Injector.obtain(applicationContext)
    fridgeItemQueryDao = Injector.obtain(applicationContext)
  }

  private fun teardown() {
    fridgeEntryQueryDao = null
    fridgeItemQueryDao = null
  }

  override fun onStopped() {
    super.onStopped()

    Timber.w("ExpirationWorker stopped")
    teardown()
  }

  private fun notifyForEntry(
    today: Calendar,
    tomorrow: Calendar,
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
            if (item.isExpiringSoon(today, tomorrow)) {
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
          .atMidnight()
      val tomorrow = Calendar.getInstance()
          .tomorrowMidnight()

      return@defer requireNotNull(fridgeEntryQueryDao).queryAll(true)
          .flatMapObservable { Observable.fromIterable(it) }
          .flatMapSingle { entry ->
            return@flatMapSingle requireNotNull(fridgeItemQueryDao).queryAll(true, entry.id())
                .doOnSuccess { items -> notifyForEntry(today, tomorrow, entry, items) }
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
    return Result.success()
  }

  @CheckResult
  private fun fail(throwable: Throwable): Result {
    Timber.e(throwable, "Butler failed to notify")
    return Result.failure()
  }
}
