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

package com.pyamsoft.fridge.entry.action

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.pydroid.core.threads.Enforcer
import io.reactivex.Single
import javax.inject.Inject

internal class EntryActionInteractor @Inject internal constructor(
  private val enforcer: Enforcer,
  private val insertDao: FridgeEntryInsertDao
) {

  @CheckResult
  fun create(): Single<FridgeEntry> {
    return Single.just(FridgeEntry.empty())
      .flatMap { entry ->
        enforcer.assertNotOnMainThread()
        return@flatMap insertDao.insert(entry)
          .andThen(Single.just(entry))
      }
  }
}