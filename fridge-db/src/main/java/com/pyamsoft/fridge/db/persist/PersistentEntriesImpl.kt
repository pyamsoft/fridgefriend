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

package com.pyamsoft.fridge.db.persist

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.guarantee.EntryGuarantee
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

internal class PersistentEntriesImpl @Inject internal constructor(
    private val guarantee: EntryGuarantee,
    private val preferences: PersistentEntryPreferences
) : PersistentEntries {

    override suspend fun getPersistentEntry(): FridgeEntry =
        withContext(context = Dispatchers.Default) {
            val possibleId = preferences.getPersistentEntryId()
            val entry = guarantee.existing(possibleId, FridgeEntry.DEFAULT_NAME)

            // If it did not previously exist, it was newly created above
            if (possibleId != entry.id()) {
                preferences.savePersistentEntryId(entry.id())
            }
            return@withContext entry
        }
}
