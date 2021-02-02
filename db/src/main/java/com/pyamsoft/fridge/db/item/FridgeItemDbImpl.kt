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

package com.pyamsoft.fridge.db.item

import com.pyamsoft.cachify.Cached
import com.pyamsoft.cachify.MultiCached1
import com.pyamsoft.cachify.MultiCached2
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.DbApi
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FridgeItemDbImpl @Inject internal constructor(
    @param:DbApi private val realAllItemsQueryDao: Cached<List<FridgeItem>>,
    @param:DbApi private val realItemsByEntryQueryDao: MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id>,
    @param:DbApi private val realSameNameDifferentPresenceQueryDao: MultiCached2<FridgeItemDb.QuerySameNameDifferentPresenceKey, List<FridgeItem>, String, Presence>,
    @param:DbApi private val realSimilarNamedQueryDao: MultiCached2<FridgeItemDb.QuerySimilarNamedKey, List<FridgeItemDb.SimilarityScore>, FridgeItem.Id, String>,
    @param:DbApi private val realInsertDao: FridgeItemInsertDao,
    @param:DbApi private val realDeleteDao: FridgeItemDeleteDao,
) : BaseDbImpl<
        FridgeItemChangeEvent,
        FridgeItemRealtime,
        FridgeItemQueryDao,
        FridgeItemInsertDao,
        FridgeItemDeleteDao
        >(), FridgeItemDb {

    override suspend fun listenForChanges(onChange: suspend (event: FridgeItemChangeEvent) -> Unit) =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            onEvent(onChange)
        }

    override suspend fun listenForChanges(
        id: FridgeEntry.Id,
        onChange: suspend (event: FridgeItemChangeEvent) -> Unit,
    ) = listenForChanges { event ->
        if (event.entryId == id) {
            onChange(event)
        }
    }

    override suspend fun query(force: Boolean): List<FridgeItem> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidate()
            }

            return@withContext realAllItemsQueryDao.call()
        }

    override suspend fun query(force: Boolean, id: FridgeEntry.Id): List<FridgeItem> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()

            if (force) {
                invalidate()
            }

            return@withContext realItemsByEntryQueryDao.key(id).call(id)
        }

    override suspend fun querySameNameDifferentPresence(
        force: Boolean,
        name: String,
        presence: Presence,
    ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        if (force) {
            invalidate()
        }

        val key = FridgeItemDb.QuerySameNameDifferentPresenceKey(name, presence)
        return@withContext realSameNameDifferentPresenceQueryDao.key(key).call(name, presence)
    }

    override suspend fun querySimilarNamedItems(
        force: Boolean,
        id: FridgeItem.Id,
        name: String,
    ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        if (force) {
            invalidate()
        }

        val key = FridgeItemDb.QuerySimilarNamedKey(id, name)
        val result = realSimilarNamedQueryDao.key(key).call(id, name)
        return@withContext if (result.isEmpty()) emptyList() else {
            result.chunked(SIMILARITY_MAX_ITEM_COUNT)
                .first()
                .map { it.item }
                .toList()
        }
    }

    override suspend fun insert(o: FridgeItem): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realInsertDao.insert(o).also { inserted ->
                if (inserted) {
                    publish(FridgeItemChangeEvent.Insert(o.makeReal()))
                } else {
                    publish(FridgeItemChangeEvent.Update(o.makeReal()))
                }
            }
        }

    override suspend fun delete(o: FridgeItem, offerUndo: Boolean): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realDeleteDao.delete(o, offerUndo).also { deleted ->
                if (deleted) {
                    publish(FridgeItemChangeEvent.Delete(o.makeReal(), offerUndo))
                }
            }
        }

    override fun realtime(): FridgeItemRealtime {
        return this
    }

    override fun queryDao(): FridgeItemQueryDao {
        return this
    }

    override fun insertDao(): FridgeItemInsertDao {
        return this
    }

    override fun deleteDao(): FridgeItemDeleteDao {
        return this
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        realAllItemsQueryDao.clear()
        realItemsByEntryQueryDao.clear()
        realSameNameDifferentPresenceQueryDao.clear()
        realSimilarNamedQueryDao.clear()
    }

    companion object {

        private const val SIMILARITY_MAX_ITEM_COUNT = 6
    }
}
