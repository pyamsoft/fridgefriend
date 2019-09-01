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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class NearbyStoreDbImpl internal constructor(
    private val db: NearbyStoreDb,
    private val cache: Cached1<Sequence<NearbyStore>, Boolean>
) : NearbyStoreDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: NearbyStoreChangeEvent) {
        cache.clear()
        publish(event)
    }

    override suspend fun publish(event: NearbyStoreChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): NearbyStoreRealtime {
        return db.realtime()
    }

    override fun queryDao(): NearbyStoreQueryDao {
        return object : NearbyStoreQueryDao {

            override suspend fun query(force: Boolean): List<NearbyStore> {
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

    override fun insertDao(): NearbyStoreInsertDao {
        return object : NearbyStoreInsertDao {

            override suspend fun insert(o: NearbyStore) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                    publishRealtime(Insert(o))
                }
            }
        }
    }

    override fun updateDao(): NearbyStoreUpdateDao {
        return object : NearbyStoreUpdateDao {

            override suspend fun update(o: NearbyStore) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                    publishRealtime(Update(o))
                }
            }
        }
    }

    override fun deleteDao(): NearbyStoreDeleteDao {
        return object : NearbyStoreDeleteDao {

            override suspend fun delete(o: NearbyStore) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                    publishRealtime(Delete(o))
                }
            }
        }
    }
}
