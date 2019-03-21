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

package com.pyamsoft.fridge.entry.list

import androidx.annotation.CheckResult
import com.popinnow.android.repo.Repo
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryScope
import com.pyamsoft.pydroid.core.cache.Cache
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

@EntryScope
internal class EntryListInteractor @Inject internal constructor(
  private val queryDao: FridgeItemQueryDao,
  private val realtime: FridgeItemRealtime,
  private val repo: Repo<List<FridgeItem>>
) : Cache {

  @CheckResult
  fun getEntries(force: Boolean): Single<List<FridgeItem>> {
    return repo.get(force) { queryDao.queryAll() }
  }

  @CheckResult
  fun listenForChanges(): Observable<FridgeItemChangeEvent> {
    return realtime.listenForChanges()
      .doOnNext { repo.clear() }
  }

  override fun clearCache() {
    repo.cancel()
  }
}