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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class DetailListInteractor @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  private val itemQueryDao: FridgeItemQueryDao,
  private val itemInsertDao: FridgeItemInsertDao,
  private val itemUpdateDao: FridgeItemUpdateDao,
  private val itemRealtime: FridgeItemRealtime,
  private val entryUpdateDao: FridgeEntryUpdateDao,
  private val entryRealtime: FridgeEntryRealtime,
  entryQueryDao: FridgeEntryQueryDao,
  entryInsertDao: FridgeEntryInsertDao,
  enforcer: Enforcer
) : DetailInteractor(enforcer, entryQueryDao, entryInsertDao) {

  @CheckResult
  fun listenForArchived(): Observable<FridgeEntry> {
    return entryRealtime.listenForChanges()
        .ofType(Update::class.java)
        .map { it.entry }
        .filter { it.id() == entryId }
        .filter { it.isArchived() }
  }

  @CheckResult
  fun observeEntryReal(force: Boolean): Observable<Boolean> {
    return listenForRealChange()
        .startWith(isEntryReal(force))
  }

  @CheckResult
  private fun isEntryReal(force: Boolean): Observable<Boolean> {
    return getEntryForId(entryId, force)
        .map { it.isReal() }
        .toObservable()
  }

  @CheckResult
  private fun listenForRealChange(): Observable<Boolean> {
    return entryRealtime.listenForChanges()
        .ofType(Insert::class.java)
        .map { it.entry }
        .filter { it.id() == entryId }
        .map { it.isReal() }
  }

  @CheckResult
  fun archive(): Completable {
    return getValidEntry(entryId, false)
        .flatMapCompletable {
          val valid = it.entry
          if (valid != null) {
            Timber.d("Archive entry: [${valid.id()}] $valid")
            return@flatMapCompletable entryUpdateDao.update(valid.archive())
          } else {
            Timber.w("No entry, cannot delete")
            return@flatMapCompletable Completable.complete()
          }
        }
  }

  @CheckResult
  fun getItems(
    entryId: String,
    force: Boolean
  ): Single<List<FridgeItem>> {
    return itemQueryDao.queryAll(force, entryId)
  }

  @CheckResult
  fun listenForChanges(entryId: String): Observable<FridgeItemChangeEvent> {
    return itemRealtime.listenForChanges(entryId)
  }

  @CheckResult
  fun commit(item: FridgeItem): Completable {
    if (item.name().isBlank()) {
      Timber.w("Do not commit empty name FridgeItem: $item")
      return Completable.complete()
    } else {
      return guaranteeEntryExists(item.entryId())
          .flatMapCompletable { commitItem(item) }
    }
  }

  @CheckResult
  private fun commitItem(item: FridgeItem): Completable {
    return getItems(item.entryId(), false)
        .flatMapObservable {
          enforcer.assertNotOnMainThread()
          return@flatMapObservable Observable.fromIterable(it)
        }
        .filter { it.id() == item.id() }
        .map { ValidItem(it) }
        .single(ValidItem(null))
        .flatMapCompletable {
          enforcer.assertNotOnMainThread()
          val valid = it.item
          if (valid != null) {
            Timber.d("Update existing item [${item.id()}]: $item")
            return@flatMapCompletable itemUpdateDao.update(item)
          } else {
            Timber.d("Create new item [${item.id()}]: $item")
            return@flatMapCompletable itemInsertDao.insert(item)
          }
        }
  }

  @CheckResult
  fun archive(item: FridgeItem): Completable {
    if (!item.isReal()) {
      Timber.w("Cannot archive item that is not real: [${item.id()}]: $item")
      return Completable.complete()
    } else {
      Timber.d("Archiving item [${item.id()}]: $item")
      return itemUpdateDao.update(item.archive())
    }
  }

  private data class ValidItem(val item: FridgeItem?)
}
