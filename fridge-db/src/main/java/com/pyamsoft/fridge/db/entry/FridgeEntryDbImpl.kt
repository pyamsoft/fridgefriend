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

package com.pyamsoft.fridge.db.entry

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Delete
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class FridgeEntryDbImpl internal constructor(
    enforcer: Enforcer,
    cache: Cached1<Sequence<FridgeEntry>, Boolean>,
    insertDao: FridgeEntryInsertDao,
    updateDao: FridgeEntryUpdateDao,
    deleteDao: FridgeEntryDeleteDao
) : BaseDbImpl<FridgeEntry, FridgeEntryChangeEvent>(enforcer, cache), FridgeEntryDb {

    private val mutex = Mutex()

    private val realtime = object : FridgeEntryRealtime {
        override suspend fun listenForChanges(onChange: suspend (event: FridgeEntryChangeEvent) -> Unit) =
            withContext(context = Dispatchers.IO) { onEvent(onChange) }
    }

    private val queryDao = object : FridgeEntryQueryDao {

        override suspend fun query(force: Boolean): List<FridgeEntry> =
            withContext(context = Dispatchers.IO) {
                enforcer.assertNotOnMainThread()
                mutex.withLock {
                    if (force) {
                        invalidate()
                    }

                    return@withContext cache.call(force).toList()
                }
            }
    }

    private val insertDao = object : FridgeEntryInsertDao {

        override suspend fun insert(o: FridgeEntry) = withContext(context = Dispatchers.IO) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { insertDao.insert(o) }
            publishRealtime(Insert(o.makeReal()))
        }
    }

    private val updateDao = object : FridgeEntryUpdateDao {

        override suspend fun update(o: FridgeEntry) = withContext(context = Dispatchers.IO) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { updateDao.update(o) }
            publishRealtime(Update(o.makeReal()))
        }
    }

    private val deleteDao = object : FridgeEntryDeleteDao {

        override suspend fun delete(o: FridgeEntry) = withContext(context = Dispatchers.IO) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { deleteDao.delete(o) }
            publishRealtime(Delete(o.makeReal()))
        }

        override suspend fun deleteAll() = withContext(context = Dispatchers.IO) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { deleteDao.deleteAll() }
            publishRealtime(DeleteAll)
        }
    }

    override fun realtime(): FridgeEntryRealtime {
        return realtime
    }

    override fun queryDao(): FridgeEntryQueryDao {
        return queryDao
    }

    override fun insertDao(): FridgeEntryInsertDao {
        return insertDao
    }

    override fun updateDao(): FridgeEntryUpdateDao {
        return updateDao
    }

    override fun deleteDao(): FridgeEntryDeleteDao {
        return deleteDao
    }
}
