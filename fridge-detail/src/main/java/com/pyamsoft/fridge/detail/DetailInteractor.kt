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
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.pydroid.arch.EventConsumer
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

internal class DetailInteractor @Inject internal constructor(
    private val itemQueryDao: FridgeItemQueryDao,
    private val itemInsertDao: FridgeItemInsertDao,
    private val itemUpdateDao: FridgeItemUpdateDao,
    private val itemDeleteDao: FridgeItemDeleteDao,
    private val itemRealtime: FridgeItemRealtime,
    private val enforcer: Enforcer,
    private val queryDao: FridgeEntryQueryDao,
    private val insertDao: FridgeEntryInsertDao
) {

    @CheckResult
    suspend fun findSameNamedItems(name: String, presence: Presence): Collection<FridgeItem> {
        return itemQueryDao.querySameNameDifferentPresence(false, name, presence)
    }

    @CheckResult
    suspend fun findSimilarNamedItems(item: FridgeItem): Collection<FridgeItem> {
        return itemQueryDao.querySimilarNamedItems(false, item)
    }

    @CheckResult
    private suspend fun getEntryForId(
        entryId: String,
        force: Boolean
    ): FridgeEntry? {
        return queryDao.query(force)
            .singleOrNull { it.id() == entryId }
    }

    @CheckResult
    private suspend fun guaranteeEntryExists(
        entryId: String,
        name: String
    ): FridgeEntry {
        val valid = getEntryForId(entryId, false)
        return if (valid != null) {
            Timber.d("Entry exists, ignore: ${valid.id()}")
            valid
        } else {
            val createdTime = Calendar.getInstance()
                .time
            Timber.d("Create entry: $entryId at $createdTime")
            val newEntry =
                FridgeEntry.create(entryId, name, createdTime, isReal = true)
            insertDao.insert(newEntry)
            newEntry
        }
    }

    @CheckResult
    suspend fun getItems(
        entryId: String,
        force: Boolean
    ): List<FridgeItem> = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        return@withContext itemQueryDao.query(force, entryId)
    }

    @CheckResult
    fun listenForChanges(entryId: String): EventConsumer<FridgeItemChangeEvent> {
        return itemRealtime.listenForChanges(entryId)
    }

    suspend fun commit(item: FridgeItem) = withContext(context = Dispatchers.Default) {
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

    @CheckResult
    private fun now(): Date {
        return Calendar.getInstance()
            .time
    }

    suspend fun consume(item: FridgeItem) = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        if (!item.isReal()) {
            Timber.w("Cannot consume item that is not real: [${item.id()}]: $item")
        } else {
            Timber.d("Consuming item [${item.id()}]: $item")
            itemUpdateDao.update(item.consume(now()))
        }
    }

    suspend fun spoil(item: FridgeItem) = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        if (!item.isReal()) {
            Timber.w("Cannot spoil item that is not real: [${item.id()}]: $item")
        } else {
            Timber.d("Spoiling item [${item.id()}]: $item")
            itemUpdateDao.update(item.spoil(now()))
        }
    }

    suspend fun delete(item: FridgeItem) = withContext(context = Dispatchers.Default) {
        enforcer.assertNotOnMainThread()
        if (!item.isReal()) {
            Timber.w("Cannot delete item that is not real: [${item.id()}]: $item")
        } else {
            Timber.d("Deleting item [${item.id()}]: $item")
            itemDeleteDao.delete(item)
        }
    }
}
