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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FridgeItemDbImpl internal constructor(
    private val db: FridgeItemDb,
    private val cache: Cached1<Sequence<FridgeItem>, Boolean>
) : FridgeItemDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: FridgeItemChangeEvent) {
        cache.clear()
        publish(event)
    }

    override suspend fun publish(event: FridgeItemChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): FridgeItemRealtime {
        return db.realtime()
    }

    override fun queryDao(): FridgeItemQueryDao {
        return object : FridgeItemQueryDao {

            @CheckResult
            private suspend fun queryAsSequence(force: Boolean): Sequence<FridgeItem> {
                if (force) {
                    cache.clear()
                }

                return cache.call(force)
            }

            override suspend fun query(force: Boolean): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force).toList()
                }
            }

            override suspend fun query(
                force: Boolean,
                entryId: String
            ): List<FridgeItem> {
                mutex.withLock {
                    return queryAsSequence(force)
                        .filter { it.entryId() == entryId }
                        .toList()
                }
            }
        }
    }

    override fun insertDao(): FridgeItemInsertDao {
        return object : FridgeItemInsertDao {

            override suspend fun insert(o: FridgeItem) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                    publishRealtime(Insert(o.makeReal()))
                }
            }
        }
    }

    override fun updateDao(): FridgeItemUpdateDao {
        return object : FridgeItemUpdateDao {

            override suspend fun update(o: FridgeItem) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                    publishRealtime(Update(o.makeReal()))
                }
            }
        }
    }

    override fun deleteDao(): FridgeItemDeleteDao {
        return object : FridgeItemDeleteDao {

            override suspend fun delete(o: FridgeItem) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                    publishRealtime(Delete(o.makeReal()))
                }
            }
        }
    }
}
