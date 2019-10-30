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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class NearbyZoneDbImpl internal constructor(
    private val db: NearbyZoneDb,
    private val cache: Cached1<Sequence<NearbyZone>, Boolean>
) : NearbyZoneDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: NearbyZoneChangeEvent) {
        cache.clear()
        publish(event)
    }

    override suspend fun publish(event: NearbyZoneChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): NearbyZoneRealtime {
        return db.realtime()
    }

    override fun queryDao(): NearbyZoneQueryDao {
        return object : NearbyZoneQueryDao {

            override suspend fun query(force: Boolean): List<NearbyZone> {
                mutex.withLock {
                    if (force) {
                        cache.clear()
                    }

                    return cache.call(force)
                        .toList()
                }
            }
        }
    }

    override fun insertDao(): NearbyZoneInsertDao {
        return object : NearbyZoneInsertDao {

            override suspend fun insert(o: NearbyZone) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                }
                publishRealtime(Insert(o))
            }
        }
    }

    override fun updateDao(): NearbyZoneUpdateDao {
        return object : NearbyZoneUpdateDao {

            override suspend fun update(o: NearbyZone) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                }
                publishRealtime(Update(o))
            }
        }
    }

    override fun deleteDao(): NearbyZoneDeleteDao {
        return object : NearbyZoneDeleteDao {

            override suspend fun delete(o: NearbyZone) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                }
                publishRealtime(Delete(o))
            }
        }
    }
}
