/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.db.guarantee

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import javax.inject.Inject

internal class EntryGuaranteeImpl @Inject internal constructor(
    private val enforcer: Enforcer,
    private val queryDao: FridgeEntryQueryDao,
    private val insertDao: FridgeEntryInsertDao
) : EntryGuarantee {

    @CheckResult
    private suspend fun getEntryForId(id: FridgeEntry.Id): FridgeEntry? {
        if (id.isEmpty()) {
            Timber.w("Cannot find an entry with a blank id")
            return null
        }

        val entries = queryDao.query(false)
        return entries.singleOrNull { it.id() == id }
    }

    override suspend fun guaranteeExists(id: FridgeEntry.Id, name: String): FridgeEntry {
        enforcer.assertNotOnMainThread()
        val entry = getEntryForId(id)
        return if (entry != null) entry else {
            Timber.d("Create entry: $id")
            val newEntry = FridgeEntry.create(id, name)
            insertDao.insert(newEntry)
            newEntry
        }
    }
}
