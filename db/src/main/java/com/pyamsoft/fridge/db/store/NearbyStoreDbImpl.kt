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

package com.pyamsoft.fridge.db.store

import com.pyamsoft.cachify.Cached
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.DbApi
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class NearbyStoreDbImpl @Inject internal constructor(
    @param:DbApi private val realQueryDao: Cached<List<NearbyStore>>,
    @param:DbApi private val realInsertDao: NearbyStoreInsertDao,
    @param:DbApi private val realDeleteDao: NearbyStoreDeleteDao,
) : BaseDbImpl<
        NearbyStoreChangeEvent,
        NearbyStoreRealtime,
        NearbyStoreQueryDao,
        NearbyStoreInsertDao,
        NearbyStoreDeleteDao
        >(), NearbyStoreDb {

    override suspend fun listenForChanges(onChange: suspend (event: NearbyStoreChangeEvent) -> Unit) {
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            onEvent(onChange)
        }
    }

    override suspend fun query(force: Boolean): List<NearbyStore> =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidate()
            }

            return@withContext realQueryDao.call()
        }

    override suspend fun insert(o: NearbyStore): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realInsertDao.insert(o).also { inserted ->
                if (inserted) {
                    publish(NearbyStoreChangeEvent.Insert(o))
                } else {
                    publish(NearbyStoreChangeEvent.Update(o))
                }
            }
        }

    override suspend fun delete(o: NearbyStore, offerUndo: Boolean): Boolean =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext realDeleteDao.delete(o, offerUndo).also { deleted ->
                if (deleted) {
                    publish(NearbyStoreChangeEvent.Delete(o, offerUndo))
                }
            }
        }

    override fun realtime(): NearbyStoreRealtime {
        return this
    }

    override fun queryDao(): NearbyStoreQueryDao {
        return this
    }

    override fun insertDao(): NearbyStoreInsertDao {
        return this
    }

    override fun deleteDao(): NearbyStoreDeleteDao {
        return this
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        realQueryDao.clear()
    }
}
