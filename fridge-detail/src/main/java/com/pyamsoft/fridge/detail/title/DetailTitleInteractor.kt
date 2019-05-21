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

package com.pyamsoft.fridge.detail.title

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Completable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

internal class DetailTitleInteractor @Inject internal constructor(
  private val updateDao: FridgeEntryUpdateDao,
  enforcer: Enforcer,
  queryDao: FridgeEntryQueryDao,
  insertDao: FridgeEntryInsertDao,
  @Named("detail_entry_id") private val entryId: String
) : DetailInteractor(enforcer, queryDao, insertDao) {

  @CheckResult
  fun getEntryName(force: Boolean): Single<String> {
    return getEntryForId(entryId, force).map { it.name() }
        .toSingle("")
  }

  @CheckResult
  fun saveName(name: String): Completable {
    return getValidEntry(entryId, false)
        .flatMapCompletable {
          val valid = it.entry
          if (valid != null) {
            return@flatMapCompletable update(valid, name)
          } else {
            Timber.d("saveName called but Entry does not exist, create it")
            return@flatMapCompletable guaranteeEntryExists(entryId, name).ignoreElement()
          }
        }
  }

  @CheckResult
  private fun update(
    entry: FridgeEntry,
    name: String
  ): Completable {
    Timber.d("Updating entry name [${entry.id()}]: $name")
    enforcer.assertNotOnMainThread()
    return updateDao.update(entry.name(name))
  }
}
