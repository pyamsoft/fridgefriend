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
 *
 */

package com.pyamsoft.fridge.db.category

import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Delete
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Insert
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FridgeCategoryDbImpl internal constructor(
    private val db: FridgeCategoryDb,
    private val dbQuery: suspend (force: Boolean) -> Sequence<FridgeCategory>
) : FridgeCategoryDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: FridgeCategoryChangeEvent) {
        invalidate()
        publish(event)
    }

    override fun invalidate() {
        db.invalidate()
    }

    override suspend fun publish(event: FridgeCategoryChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): FridgeCategoryRealtime {
        return db.realtime()
    }

    override fun queryDao(): FridgeCategoryQueryDao {
        return object : FridgeCategoryQueryDao {

            override suspend fun query(force: Boolean): List<FridgeCategory> {
                mutex.withLock {
                    if (force) {
                        invalidate()
                    }

                    return dbQuery(force).toList()
                }
            }
        }
    }

    override fun insertDao(): FridgeCategoryInsertDao {
        return object : FridgeCategoryInsertDao {

            override suspend fun insert(o: FridgeCategory) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                    publishRealtime(Insert(o))
                }
            }
        }
    }

    override fun updateDao(): FridgeCategoryUpdateDao {
        return object : FridgeCategoryUpdateDao {

            override suspend fun update(o: FridgeCategory) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                    publishRealtime(Update(o))
                }
            }
        }
    }

    override fun deleteDao(): FridgeCategoryDeleteDao {
        return object : FridgeCategoryDeleteDao {

            override suspend fun delete(o: FridgeCategory) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                    publishRealtime(Delete(o))
                }
            }
        }
    }
}
