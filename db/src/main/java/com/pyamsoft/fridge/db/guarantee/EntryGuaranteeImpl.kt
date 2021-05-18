/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.db.guarantee

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class EntryGuaranteeImpl
@Inject
internal constructor(
    private val queryDao: FridgeEntryQueryDao,
    private val insertDao: FridgeEntryInsertDao
) : EntryGuarantee {

  @CheckResult
  private suspend fun getEntryForId(id: FridgeEntry.Id): FridgeEntry? {
    Enforcer.assertOffMainThread()
    if (id.isEmpty()) {
      Timber.w("Cannot find an entry with a blank id")
      return null
    }

    val entries = queryDao.query(false)
    return entries.singleOrNull { it.id() == id }
  }

  override suspend fun existing(id: FridgeEntry.Id, name: String): FridgeEntry =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        val entry = getEntryForId(id)
        return@withContext if (entry != null) entry
        else {
          Timber.d("Create new persistent entry")
          val newEntry = FridgeEntry.create(name)
          if (insertDao.insert(newEntry)) {
            Timber.d("New persistent entry saved: $newEntry")
          } else {
            Timber.d("Existing persistent entry updated: $newEntry")
          }
          newEntry
        }
      }
}
