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
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject

internal class PersistentEntriesImpl @Inject internal constructor(
    queryDao: FridgeEntryQueryDao,
    insertDao: FridgeEntryInsertDao,
    enforcer: Enforcer,
    private val preferences: PersistentEntryPreferences
) : EntryGuarantee(enforcer, queryDao, insertDao), PersistentEntries {

    override suspend fun getPersistentEntry(): FridgeEntry {
        val possibleId = preferences.getPersistentEntryId()
        val entry = guaranteeEntryExists(possibleId, FridgeEntry.DEFAULT_NAME)

        // If it did not previously exist, it was newly created above
        if (possibleId.isBlank()) {
            preferences.savePersistentEntryId(entry.id())
        }
        return entry
    }
}
