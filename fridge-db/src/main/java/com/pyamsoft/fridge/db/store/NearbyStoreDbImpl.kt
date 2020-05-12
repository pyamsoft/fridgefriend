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
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Delete
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Insert
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Update
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class NearbyStoreDbImpl internal constructor(
    private val enforcer: Enforcer,
    private val cache: Cached1<Sequence<NearbyStore>, Boolean>,
    insertDao: NearbyStoreInsertDao,
    updateDao: NearbyStoreUpdateDao,
    deleteDao: NearbyStoreDeleteDao
) : NearbyStoreDb {

    private val mutex = Mutex()

    private val bus = EventBus.create<NearbyStoreChangeEvent>()

    private val realtime = object : NearbyStoreRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: NearbyStoreChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) { bus.onEvent(onChange) }
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
            mutex.withLock {
                insertDao.insert(o)
                publishRealtime(Insert(o))
            }
        }
    }

    private val updateDao = object : NearbyStoreUpdateDao {

        override suspend fun update(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                updateDao.update(o)
            }
            publishRealtime(Update(o))
        }
    }

    private val deleteDao = object : NearbyStoreDeleteDao {

        override suspend fun delete(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                deleteDao.delete(o)
            }
            publishRealtime(Delete(o))
        }
    }

    private suspend fun publishRealtime(event: NearbyStoreChangeEvent) {
        enforcer.assertNotOnMainThread()
        invalidate()
        publish(event)
    }

    override fun invalidate() {
        cache.clear()
    }

    override suspend fun publish(event: NearbyStoreChangeEvent) {
        enforcer.assertNotOnMainThread()
        bus.publish(event)
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
