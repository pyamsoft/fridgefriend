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

package com.pyamsoft.fridge.db.room

import android.content.Context
import androidx.annotation.CheckResult
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.db.PersistentEntries
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

internal class RoomPersistentEntries @Inject internal constructor(
  context: Context,
  private val enforcer: Enforcer,
  private val queryDao: FridgeEntryQueryDao,
  private val insertDao: FridgeEntryInsertDao
) : PersistentEntries {

  private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(context) }

  @CheckResult
  private suspend fun getEntryForId(
    entryId: String,
    force: Boolean
  ): FridgeEntry? {
    val entries = queryDao.query(force)
    return entries.singleOrNull { it.id() == entryId }
  }

  @CheckResult
  private suspend fun getValidEntry(
    entryId: String,
    force: Boolean
  ): ValidEntry {
    val entry = getEntryForId(entryId, force)
    return ValidEntry(entry)
  }

  @CheckResult
  private suspend fun guaranteeEntryExists(
    key: String,
    name: String
  ): FridgeEntry {
    val entryId = getEntryId(key)
    val entry = getValidEntry(entryId, false)
    val valid = entry.entry
    if (valid != null) {
      Timber.d("Entry exists, ignore: ${valid.id()}")
      return valid
    } else {
      val createdTime = Calendar.getInstance()
          .time
      Timber.d("Create entry: $entryId at $createdTime")
      val newEntry = FridgeEntry.create(entryId, name, createdTime, isReal = true)
      insertDao.insert(newEntry)
      sharedPreferences.edit { putString(key, entryId) }
      return newEntry
    }
  }

  @CheckResult
  private fun getEntryId(key: String): String {
    return requireNotNull(sharedPreferences.getString(key, FridgeEntry.create().id()))
  }

  override suspend fun getPersistentEntry(): FridgeEntry {
    enforcer.assertNotOnMainThread()
    return guaranteeEntryExists(PERSIST_ENTRY_KEY, "Items")
  }

  private data class ValidEntry(val entry: FridgeEntry?)

  companion object {

    private const val PERSIST_ENTRY_KEY = "persistent_entry"

  }
}
