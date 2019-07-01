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
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.core.Enforcer
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

internal class DetailInteractor @Inject internal constructor(
  entry: FridgeEntry,
  private val itemQueryDao: FridgeItemQueryDao,
  private val itemInsertDao: FridgeItemInsertDao,
  private val itemUpdateDao: FridgeItemUpdateDao,
  private val itemDeleteDao: FridgeItemDeleteDao,
  private val itemRealtime: FridgeItemRealtime,
  private val entryUpdateDao: FridgeEntryUpdateDao,
  private val entryRealtime: FridgeEntryRealtime,
  private val enforcer: Enforcer,
  private val queryDao: FridgeEntryQueryDao,
  private val insertDao: FridgeEntryInsertDao
) {

  private val entryId = entry.id()

  @CheckResult
  private suspend fun getEntryForId(
    entryId: String,
    force: Boolean
  ): FridgeEntry? {
    return queryDao.queryAll(force)
        .singleOrNull { it.id() == entryId }
  }

  @CheckResult
  private suspend fun guaranteeEntryExists(
    entryId: String,
    name: String
  ): FridgeEntry {
    val valid = getEntryForId(entryId, false)
    if (valid != null) {
      Timber.d("Entry exists, ignore: ${valid.id()}")
      return valid
    } else {
      val createdTime = Calendar.getInstance()
          .time
      Timber.d("Create entry: $entryId at $createdTime")
      val newEntry =
        FridgeEntry.create(entryId, name, createdTime, isReal = true, isArchived = false)
      insertDao.insert(newEntry)
      return newEntry
    }
  }

  @CheckResult
  fun listenForEntryArchived(): EventConsumer<FridgeEntry> {
    return object : EventConsumer<FridgeEntry> {
      override suspend fun onEvent(emitter: suspend (event: FridgeEntry) -> Unit) {
        enforcer.assertNotOnMainThread()
        entryRealtime.listenForChanges()
            .onEvent stop@{ event ->
              if (event !is Update) {
                return@stop
              }

              val entry = event.entry
              if (entry.id() != entryId || !entry.isArchived()) {
                return@stop
              }

              emitter(entry)
            }
      }

    }
  }

  @CheckResult
  fun observeEntry(force: Boolean): EventConsumer<FridgeEntry> {
    return listenForEntryCreated { getEntryForId(entryId, force) }
  }

  @CheckResult
  private fun listenForEntryCreated(startWith: suspend () -> FridgeEntry?): EventConsumer<FridgeEntry> {
    return object : EventConsumer<FridgeEntry> {
      override suspend fun onEvent(emitter: suspend (event: FridgeEntry) -> Unit) {
        enforcer.assertNotOnMainThread()
        val start = startWith()
        if (start != null) {
          emitter(start)
        }
        entryRealtime.listenForChanges()
            .onEvent stop@{ event ->
              if (event !is Insert) {
                return@stop
              }

              val entry = event.entry
              if (entry.id() != entryId) {
                return@stop
              }

              emitter(entry)
            }
      }

    }
  }

  suspend fun archiveEntry() {
    enforcer.assertNotOnMainThread()
    val valid = getEntryForId(entryId, false)
    if (valid != null) {
      Timber.d("Archive entry: [${valid.id()}] $valid")
      entryUpdateDao.update(valid.archive())
    } else {
      Timber.w("No entry, cannot archive")
    }
  }

  @CheckResult
  suspend fun getItems(
    entryId: String,
    force: Boolean
  ): List<FridgeItem> {
    enforcer.assertNotOnMainThread()
    return itemQueryDao.queryAll(force, entryId)
  }

  @CheckResult
  fun listenForChanges(entryId: String): EventConsumer<FridgeItemChangeEvent> {
    return itemRealtime.listenForChanges(entryId)
  }

  suspend fun commit(item: FridgeItem) {
    enforcer.assertNotOnMainThread()
    if (item.name().isBlank()) {
      Timber.w("Do not commit empty name FridgeItem: $item")
    } else {
      guaranteeEntryExists(item.entryId(), FridgeEntry.EMPTY_NAME)
      commitItem(item)
    }
  }

  private suspend fun commitItem(item: FridgeItem) {
    val valid = getItems(item.entryId(), false)
        .singleOrNull { it.id() == item.id() }
    if (valid != null) {
      Timber.d("Update existing item [${item.id()}]: $item")
      itemUpdateDao.update(item)
    } else {
      Timber.d("Create new item [${item.id()}]: $item")
      itemInsertDao.insert(item)
    }
  }

  suspend fun archive(item: FridgeItem) {
    enforcer.assertNotOnMainThread()
    if (!item.isReal()) {
      Timber.w("Cannot archive item that is not real: [${item.id()}]: $item")
    } else {
      Timber.d("Archiving item [${item.id()}]: $item")
      itemUpdateDao.update(item.archive())
    }
  }

  suspend fun delete(item: FridgeItem) {
    enforcer.assertNotOnMainThread()
    if (!item.isReal()) {
      Timber.w("Cannot delete item that is not real: [${item.id()}]: $item")
    } else {
      Timber.d("Deleting item [${item.id()}]: $item")
      itemDeleteDao.delete(item)
    }
  }

  suspend fun saveName(name: String) {
    enforcer.assertNotOnMainThread()
    val valid = getEntryForId(entryId, false)
    if (valid != null) {
      Timber.d("Updating entry name [${valid.id()}]: $name")
      entryUpdateDao.update(valid.name(name))
    } else {
      Timber.d("saveName called but Entry does not exist, create it")
      guaranteeEntryExists(entryId, name)
    }
  }
}
