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

package com.pyamsoft.fridge.db.item

import com.pyamsoft.cachify.Cached
import com.pyamsoft.cachify.MultiCached1
import com.pyamsoft.cachify.MultiCached2
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class FridgeItemDbImpl internal constructor(
    private val allItemsCache: Cached<List<FridgeItem>>,
    private val itemsByEntryCache: MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id>,
    private val sameNameDifferentPresenceCache: MultiCached2<FridgeItemDb.QuerySameNameDifferentPresenceKey, List<FridgeItem>, String, Presence>,
    private val similarNamedCache: MultiCached2<FridgeItemDb.QuerySimilarNamedKey, List<FridgeItemDb.SimilarityScore>, FridgeItem.Id, String>,
    insertDao: FridgeItemInsertDao,
    deleteDao: FridgeItemDeleteDao
) : BaseDbImpl<FridgeItem, FridgeItemChangeEvent>(), FridgeItemDb {

    private val realtime = object : FridgeItemRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: FridgeItemChangeEvent) -> Unit) =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                onEvent(onChange)
            }

        override suspend fun listenForChanges(
            id: FridgeEntry.Id,
            onChange: suspend (event: FridgeItemChangeEvent) -> Unit
        ) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            onEvent { event ->
                if (event.entryId == id) {
                    onChange(event)
                }
            }
        }
    }

    private val queryDao = object : FridgeItemQueryDao {

        override suspend fun query(force: Boolean): List<FridgeItem> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                if (force) {
                    invalidateAllItemsCache()
                }

                return@withContext allItemsCache.call()
            }

        override suspend fun query(force: Boolean, id: FridgeEntry.Id): List<FridgeItem> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()

                if (force) {
                    invalidateItemsByEntryCache()
                }

                return@withContext itemsByEntryCache.key(id).call(id)
            }

        override suspend fun querySameNameDifferentPresence(
            force: Boolean,
            name: String,
            presence: Presence
        ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidateSameNameDifferentPresenceCache()
            }

            val key = FridgeItemDb.QuerySameNameDifferentPresenceKey(name, presence)
            return@withContext sameNameDifferentPresenceCache.key(key).call(name, presence)
        }

        override suspend fun querySimilarNamedItems(
            force: Boolean,
            id: FridgeItem.Id,
            name: String,
        ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidateSimilarNamedCache()
            }

            val key = FridgeItemDb.QuerySimilarNamedKey(id, name)
            val result = similarNamedCache.key(key).call(id, name)
            return@withContext if (result.isEmpty()) emptyList() else {
                result.chunked(SIMILARITY_MAX_ITEM_COUNT)
                    .first()
                    .map { it.item }
                    .toList()
            }
        }
    }

    private val insertDao = object : FridgeItemInsertDao {

        override suspend fun insert(o: FridgeItem): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext insertDao.insert(o).also { inserted ->
                    if (inserted) {
                        publish(FridgeItemChangeEvent.Insert(o.makeReal()))
                    } else {
                        publish(FridgeItemChangeEvent.Update(o.makeReal()))
                    }
                }
            }
    }

    private val deleteDao = object : FridgeItemDeleteDao {

        override suspend fun delete(o: FridgeItem): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext deleteDao.delete(o).also { deleted ->
                    if (deleted) {
                        publish(FridgeItemChangeEvent.Delete(o.makeReal()))
                    }
                }
            }
    }

    override fun realtime(): FridgeItemRealtime {
        return realtime
    }

    override fun queryDao(): FridgeItemQueryDao {
        return queryDao
    }

    override fun insertDao(): FridgeItemInsertDao {
        return insertDao
    }

    override fun deleteDao(): FridgeItemDeleteDao {
        return deleteDao
    }

    private suspend fun invalidateAllItemsCache() {
        allItemsCache.clear()
    }

    private suspend fun invalidateItemsByEntryCache() {
        itemsByEntryCache.clear()
    }

    private suspend fun invalidateSameNameDifferentPresenceCache() {
        sameNameDifferentPresenceCache.clear()
    }

    private suspend fun invalidateSimilarNamedCache() {
        similarNamedCache.clear()
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        invalidateAllItemsCache()
        invalidateItemsByEntryCache()
        invalidateSameNameDifferentPresenceCache()
        invalidateSimilarNamedCache()
    }

    companion object {

        private const val SIMILARITY_MAX_ITEM_COUNT = 6
    }
}
