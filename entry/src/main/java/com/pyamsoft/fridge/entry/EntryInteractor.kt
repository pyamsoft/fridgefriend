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
 */

package com.pyamsoft.fridge.entry

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryInteractor @Inject internal constructor(
    private val entryInsertDao: FridgeEntryInsertDao,
    private val entryQueryDao: FridgeEntryQueryDao,
    private val entryRealtime: FridgeEntryRealtime
) {

    @CheckResult
    suspend fun loadEntries(force: Boolean): List<FridgeEntry> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext entryQueryDao.query(force)
        }

    suspend fun listenForChanges(onChange: suspend (FridgeEntryChangeEvent) -> Unit) =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext entryRealtime.listenForChanges(onChange)
        }

    suspend fun createEntry(name: String) = withContext(context = Dispatchers.IO) {
        require(name.isNotBlank()) { "Name cannot be blank" }

        val entry = FridgeEntry.create(name)
        entryInsertDao.insert(entry.makeReal())
    }

}