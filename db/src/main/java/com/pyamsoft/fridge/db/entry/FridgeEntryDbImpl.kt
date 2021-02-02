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

package com.pyamsoft.fridge.db.entry

import com.pyamsoft.cachify.Cached
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.DbApi
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FridgeEntryDbImpl @Inject internal constructor(
    @param:DbApi private val realQueryDao: Cached<List<FridgeEntry>>,
    @param:DbApi private val realInsertDao: FridgeEntryInsertDao,
    @param:DbApi private val realDeleteDao: FridgeEntryDeleteDao,
) : BaseDbImpl<
        FridgeEntryChangeEvent,
        FridgeEntryRealtime,
        FridgeEntryQueryDao,
        FridgeEntryInsertDao,
        FridgeEntryDeleteDao
        >(), FridgeEntryDb {

    override suspend fun listenForChanges(onChange: suspend (event: FridgeEntryChangeEvent) -> Unit) =
        withContext(context = Dispatchers.IO) { onEvent(onChange) }

    override suspend fun query(force: Boolean): List<FridgeEntry> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidate()
            }

            return@withContext realQueryDao.call()
        }

    override suspend fun insert(o: FridgeEntry): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realInsertDao.insert(o).also { inserted ->
                if (inserted) {
                    publish(FridgeEntryChangeEvent.Insert(o.makeReal()))
                } else {
                    publish(FridgeEntryChangeEvent.Update(o.makeReal()))
                }
            }
        }

    override suspend fun delete(o: FridgeEntry, offerUndo: Boolean): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realDeleteDao.delete(o, offerUndo).also { deleted ->
                if (deleted) {
                    publish(FridgeEntryChangeEvent.Delete(o.makeReal(), offerUndo))
                }
            }
        }

    override suspend fun deleteAll() = withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        return@withContext realDeleteDao.deleteAll().also {
            publish(FridgeEntryChangeEvent.DeleteAll)
        }
    }

    override fun realtime(): FridgeEntryRealtime {
        return this
    }

    override fun queryDao(): FridgeEntryQueryDao {
        return this
    }

    override fun insertDao(): FridgeEntryInsertDao {
        return this
    }

    override fun deleteDao(): FridgeEntryDeleteDao {
        return this
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        realQueryDao.clear()
    }
}
