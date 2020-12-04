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
import com.pyamsoft.fridge.db.fridge.BaseDbImpl
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NearbyStoreDbImpl internal constructor(
    private val cache: Cached<List<NearbyStore>>,
    insertDao: NearbyStoreInsertDao,
    deleteDao: NearbyStoreDeleteDao
) : BaseDbImpl<NearbyStore, NearbyStoreChangeEvent>(), NearbyStoreDb {

    private val realtime = object : NearbyStoreRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: NearbyStoreChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                onEvent(onChange)
            }
        }
    }

    private val queryDao = object : NearbyStoreQueryDao {

        override suspend fun query(force: Boolean): List<NearbyStore> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                if (force) {
                    invalidate()
                }

                return@withContext cache.call()
            }
    }

    private val insertDao = object : NearbyStoreInsertDao {

        override suspend fun insert(o: NearbyStore): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext insertDao.insert(o).also { inserted ->
                    if (inserted) {
                        publish(NearbyStoreChangeEvent.Insert(o))
                    } else {
                        publish(NearbyStoreChangeEvent.Update(o))
                    }
                }
            }
    }

    private val deleteDao = object : NearbyStoreDeleteDao {

        override suspend fun delete(o: NearbyStore): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext deleteDao.delete(o).also { deleted ->
                    if (deleted) {
                        publish(NearbyStoreChangeEvent.Delete(o))
                    }
                }
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

    override fun deleteDao(): NearbyStoreDeleteDao {
        return deleteDao
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        cache.clear()
    }
}
