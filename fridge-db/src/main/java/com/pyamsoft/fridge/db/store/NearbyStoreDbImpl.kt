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

package com.pyamsoft.fridge.db.store

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Delete
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Insert
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Update
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class NearbyStoreDbImpl internal constructor(
    enforcer: Enforcer,
    cache: Cached1<Sequence<NearbyStore>, Boolean>,
    insertDao: NearbyStoreInsertDao,
    updateDao: NearbyStoreUpdateDao,
    deleteDao: NearbyStoreDeleteDao
) : BaseDbImpl<NearbyStore, NearbyStoreChangeEvent>(enforcer, cache), NearbyStoreDb {

    private val mutex = Mutex()

    private val realtime = object : NearbyStoreRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: NearbyStoreChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) { onEvent(onChange) }
        }
    }

    private val queryDao = object : NearbyStoreQueryDao {

        override suspend fun query(force: Boolean): List<NearbyStore> {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                if (force) {
                    invalidate()
                }

                return cache.call(force).toList()
            }
        }
    }

    private val insertDao = object : NearbyStoreInsertDao {

        override suspend fun insert(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { insertDao.insert(o) }
            publish(Insert(o))
        }
    }

    private val updateDao = object : NearbyStoreUpdateDao {

        override suspend fun update(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { updateDao.update(o) }
            publish(Update(o))
        }
    }

    private val deleteDao = object : NearbyStoreDeleteDao {

        override suspend fun delete(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { deleteDao.delete(o) }
            publish(Delete(o))
        }
    }

    override fun realtime(): NearbyStoreRealtime {
        return realtime
    }

    override fun queryDao(): NearbyStoreQueryDao {
        return queryDao
    }

    override fun insertDao(): NearbyStoreInsertDao {
        return insertDao
    }

    override fun updateDao(): NearbyStoreUpdateDao {
        return updateDao
    }

    override fun deleteDao(): NearbyStoreDeleteDao {
        return deleteDao
    }
}
