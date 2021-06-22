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

package com.pyamsoft.fridge.entry

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.preference.EntryPreferences
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.ResultWrapper
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class EntryInteractor
@Inject
internal constructor(
    private val preferences: EntryPreferences,
    private val itemInsertDao: FridgeItemInsertDao,
    private val itemQueryDao: FridgeItemQueryDao,
    private val itemRealtime: FridgeItemRealtime,
    private val entryInsertDao: FridgeEntryInsertDao,
    private val entryQueryDao: FridgeEntryQueryDao,
    private val entryDeleteDao: FridgeEntryDeleteDao,
    private val entryRealtime: FridgeEntryRealtime,
) {

  @CheckResult
  suspend fun loadEntries(force: Boolean): ResultWrapper<List<FridgeEntry>> =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          var entries = entryQueryDao.query(force)
          if (entries.isEmpty() && !preferences.isDefaultEntryCreated()) {
            Timber.d("Create first entry when entries list is empty")
            val entry = FridgeEntry.create("My Fridge")
            if (entryInsertDao.insert(entry)) {
              Timber.d("First entry created: $entry")
              preferences.markDefaultEntryCreated()

              val items = generateItems(entry)
              for (item in items) {
                if (itemInsertDao.insert(item.makeReal())) {
                  Timber.d("Item created in first entry: $item")
                }
              }

              entries = entryQueryDao.query(force)
            }
          }

          ResultWrapper.success(entries)
        } catch (e: Throwable) {
          Timber.e(e, "Error loading entries")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun loadItems(force: Boolean, entry: FridgeEntry): ResultWrapper<List<FridgeItem>> =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          ResultWrapper.success(itemQueryDao.query(force, entry.id()))
        } catch (e: Throwable) {
          Timber.e(e, "Error loading entry items: $entry")
          ResultWrapper.failure(e)
        }
      }

  suspend fun listenForEntryChanges(onChange: suspend (FridgeEntryChangeEvent) -> Unit) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        return@withContext entryRealtime.listenForChanges(onChange)
      }

  suspend fun listenForItemChanges(onChange: suspend (FridgeItemChangeEvent) -> Unit) =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        return@withContext itemRealtime.listenForChanges(onChange)
      }

  @CheckResult
  suspend fun loadEntry(id: FridgeEntry.Id): ResultWrapper<FridgeEntry> =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          ResultWrapper.success(entryQueryDao.query(false).first { it.id() == id })
        } catch (e: Throwable) {
          Timber.e(e, "Error loading entry $id")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun delete(entry: FridgeEntry, offerUndo: Boolean): ResultWrapper<Boolean> =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        require(entry.isReal()) { "Entry must be real" }

        return@withContext try {
          ResultWrapper.success(entryDeleteDao.delete(entry, offerUndo))
        } catch (e: Throwable) {
          Timber.e(e, "Error deleting entry $entry")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun commit(entry: FridgeEntry): ResultWrapper<Boolean> =
      withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()

        require(entry.isReal()) { "Entry must be real" }
        return@withContext try {
          ResultWrapper.success(entryInsertDao.insert(entry))
        } catch (e: Throwable) {
          Timber.e(e, "Error committing entry $entry")
          ResultWrapper.failure(e)
        }
      }

  companion object {

    @CheckResult
    private fun generateItems(entry: FridgeEntry): List<FridgeItem> {
      return listOf(
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.NEED)
              .name("Need First Item"),
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.NEED)
              .name("Need Second Item"),
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.NEED)
              .name("Need Third Item"),
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.HAVE)
              .name("Have First Item")
              .purchaseTime(currentDate())
              .expireTime(today().apply { add(Calendar.DAY_OF_YEAR, 5) }.time),
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.HAVE)
              .name("Have Second Item")
              .purchaseTime(currentDate())
              .expireTime(today().apply { add(Calendar.DAY_OF_YEAR, 7) }.time),
          FridgeItem.create(entryId = entry.id(), presence = FridgeItem.Presence.HAVE)
              .name("Have Third Item")
              .purchaseTime(currentDate())
              .expireTime(today().apply { add(Calendar.DAY_OF_YEAR, 10) }.time),
      )
    }
  }
}
