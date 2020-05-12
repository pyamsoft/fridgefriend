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

package com.pyamsoft.fridge.db.zone

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Delete
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Insert
import com.pyamsoft.fridge.db.zone.NearbyZoneChangeEvent.Update
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class NearbyZoneDbImpl internal constructor(
    private val enforcer: Enforcer,
    private val cache: Cached1<Sequence<NearbyZone>, Boolean>,
    insertDao: NearbyZoneInsertDao,
    updateDao: NearbyZoneUpdateDao,
    deleteDao: NearbyZoneDeleteDao
) : NearbyZoneDb {

    private val mutex = Mutex()

    private val bus = EventBus.create<NearbyZoneChangeEvent>()

    private val realtime = object : NearbyZoneRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: NearbyZoneChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) { bus.onEvent(onChange) }
        }
    }

    private val queryDao = object : NearbyZoneQueryDao {

        override suspend fun query(force: Boolean): List<NearbyZone> {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                if (force) {
                    invalidate()
                }

                return cache.call(force).toList()
            }
        }
    }

    private val insertDao = object : NearbyZoneInsertDao {

        override suspend fun insert(o: NearbyZone) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { insertDao.insert(o) }
            publishRealtime(Insert(o))
        }
    }

    private val updateDao = object : NearbyZoneUpdateDao {

        override suspend fun update(o: NearbyZone) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { updateDao.update(o) }
            publishRealtime(Update(o))
        }
    }

    private val deleteDao = object : NearbyZoneDeleteDao {

        override suspend fun delete(o: NearbyZone) {
            enforcer.assertNotOnMainThread()
            mutex.withLock { deleteDao.delete(o) }
            publishRealtime(Delete(o))
        }
    }

    private suspend fun publishRealtime(event: NearbyZoneChangeEvent) {
        enforcer.assertNotOnMainThread()
        invalidate()
        publish(event)
    }

    override fun invalidate() {
        cache.clear()
    }

    override suspend fun publish(event: NearbyZoneChangeEvent) {
        enforcer.assertNotOnMainThread()
        bus.publish(event)
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

    override fun updateDao(): NearbyZoneUpdateDao {
        return updateDao
    }

    override fun deleteDao(): NearbyZoneDeleteDao {
        return deleteDao
    }
}
