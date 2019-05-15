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

package com.pyamsoft.fridge.detail.create.toolbar

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Observable
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class CreationToolbarInteractor @Inject internal constructor(
  private val updateDao: FridgeEntryUpdateDao,
  private val realtime: FridgeEntryRealtime,
  enforcer: Enforcer,
  queryDao: FridgeEntryQueryDao,
  insertDao: FridgeEntryInsertDao,
  @Named("detail_entry_id") private val entryId: String
) : DetailInteractor(enforcer, queryDao, insertDao) {

  @CheckResult
  fun listenForArchived(): Observable<FridgeEntry> {
    return realtime.listenForChanges()
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
    return realtime.listenForChanges()
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
            return@flatMapCompletable updateDao.update(valid.archive())
          } else {
            Timber.w("No entry, cannot delete")
            return@flatMapCompletable Completable.complete()
          }
        }
  }

}
