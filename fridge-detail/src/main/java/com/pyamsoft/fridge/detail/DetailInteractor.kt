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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.pydroid.core.optional.Optional
import com.pyamsoft.pydroid.core.optional.Optional.Present
import com.pyamsoft.pydroid.core.optional.asOptional
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import java.util.Date

internal abstract class DetailInteractor protected constructor(
  protected val enforcer: Enforcer,
  private val queryDao: FridgeEntryQueryDao,
  private val insertDao: FridgeEntryInsertDao,
  protected val entryId: String
) {

  @CheckResult
  protected fun getEntryForId(force: Boolean): Maybe<FridgeEntry> {
    return queryDao.queryAll(force)
      .flatMapObservable {
        enforcer.assertNotOnMainThread()
        return@flatMapObservable Observable.fromIterable(it)
      }
      .filter { it.id() == entryId }
      .singleElement()
  }

  @CheckResult
  @JvmOverloads
  protected fun guaranteeEntryExists(
    name: String = FridgeEntry.EMPTY_NAME,
    createdTime: Date = Date(0)
  ): Single<FridgeEntry> {
    return getEntryForId(false)
      .map { it.asOptional() }
      .toSingle(Optional.ofNullable(null))
      .flatMap {
        enforcer.assertNotOnMainThread()
        if (it is Present) {
          return@flatMap Single.just(it.value)
        } else {
          val entry = FridgeEntry.create(entryId, name, createdTime)
          return@flatMap insertDao.insert(entry)
            .andThen(Single.just(entry))
        }
      }
  }
}