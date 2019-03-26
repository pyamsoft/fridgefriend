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

package com.pyamsoft.fridge.detail.list

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class DetailListInteractor @Inject internal constructor(
  private val queryDao: FridgeItemQueryDao,
  private val insertDao: FridgeItemInsertDao,
  private val updateDao: FridgeItemUpdateDao,
  private val realtime: FridgeItemRealtime,
  entryQueryDao: FridgeEntryQueryDao,
  entryInsertDao: FridgeEntryInsertDao,
  enforcer: Enforcer,
  @Named("detail_entry_id") entryId: String
) : DetailInteractor(enforcer, entryQueryDao, entryInsertDao, entryId) {

  @CheckResult
  fun getItems(force: Boolean): Single<List<FridgeItem>> {
    return queryDao.queryAll(force, entryId)
  }

  @CheckResult
  fun listenForChanges(): Observable<FridgeItemChangeEvent> {
    return realtime.listenForChanges(entryId)
  }

  @CheckResult
  fun commit(item: FridgeItem, finalUpdate: Boolean): Completable {
    if (item.name().isBlank()) {
      Timber.w("Do not commit empty name FridgeItem")
      return Completable.complete()
    } else {
      return guaranteeEntryExists()
        .flatMapCompletable { commitItem(item) }
    }
  }

  @CheckResult
  private fun commitItem(item: FridgeItem): Completable {
    return getItems(false)
      .flatMapObservable {
        enforcer.assertNotOnMainThread()
        return@flatMapObservable Observable.fromIterable(it)
      }.filter { it.id() == item.id() }
      .map { it.asOptional() }
      .single(Optional.ofNullable(null))
      .flatMapCompletable {
        enforcer.assertNotOnMainThread()
        if (it is Present) {
          Timber.d("Update existing item [${item.id()}]: $item")
          return@flatMapCompletable updateDao.update(item)
        } else {
          Timber.d("Create new item [${item.id()}]: $item")
          return@flatMapCompletable insertDao.insert(item)
        }
      }
  }
}