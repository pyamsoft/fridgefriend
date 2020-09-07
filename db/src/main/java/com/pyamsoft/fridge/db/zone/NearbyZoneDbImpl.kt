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

package com.pyamsoft.fridge.db.zone

import com.pyamsoft.cachify.Cached
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class NearbyZoneDbImpl internal constructor(
    private val cache: Cached<List<NearbyZone>>,
    insertDao: NearbyZoneInsertDao,
    deleteDao: NearbyZoneDeleteDao
) : BaseDbImpl<NearbyZone, NearbyZoneChangeEvent>(), NearbyZoneDb {

    private val realtime = object : NearbyZoneRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: NearbyZoneChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) { onEvent(onChange) }
        }
    }

    private val queryDao = object : NearbyZoneQueryDao {

        override suspend fun query(force: Boolean): List<NearbyZone> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                if (force) {
                    invalidate()
                }

                return@withContext cache.call()
            }
    }

    private val insertDao = object : NearbyZoneInsertDao {

        override suspend fun insert(o: NearbyZone): Boolean =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext insertDao.insert(o).also { inserted ->
                    if (inserted) {
                        publish(NearbyZoneChangeEvent.Insert(o))
                    } else {
                        publish(NearbyZoneChangeEvent.Update(o))
                    }
                }
            }
    }

    private val deleteDao = object : NearbyZoneDeleteDao {

        override suspend fun delete(o: NearbyZone) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext deleteDao.delete(o).also { deleted ->
                if (deleted) {
                    publish(NearbyZoneChangeEvent.Delete(o))
                }
            }
        }
    }

    override fun realtime(): NearbyZoneRealtime {
        return realtime
    }

    override fun queryDao(): NearbyZoneQueryDao {
        return queryDao
    }

    override fun insertDao(): NearbyZoneInsertDao {
        return insertDao
    }

    override fun deleteDao(): NearbyZoneDeleteDao {
        return deleteDao
    }

    override suspend fun invalidate() = withContext(context = Dispatchers.IO) {
        cache.clear()
    }
}
