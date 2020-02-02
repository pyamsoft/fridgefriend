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

package com.pyamsoft.fridge.db.entry

import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Delete
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FridgeEntryDbImpl internal constructor(
    private val db: FridgeEntryDb,
    private val dbQuery: suspend (force: Boolean) -> Sequence<FridgeEntry>
) : FridgeEntryDb {

    private val mutex = Mutex()

    private suspend fun publishRealtime(event: FridgeEntryChangeEvent) {
        invalidate()
        publish(event)
    }

    override fun invalidate() {
        db.invalidate()
    }

    override suspend fun publish(event: FridgeEntryChangeEvent) {
        db.publish(event)
    }

    override fun realtime(): FridgeEntryRealtime {
        return db.realtime()
    }

    override fun queryDao(): FridgeEntryQueryDao {
        return object : FridgeEntryQueryDao {

            override suspend fun query(force: Boolean): List<FridgeEntry> {
                mutex.withLock {
                    if (force) {
                        invalidate()
                    }

                    return dbQuery(force).toList()
                }
            }
        }
    }

    override fun insertDao(): FridgeEntryInsertDao {
        return object : FridgeEntryInsertDao {

            override suspend fun insert(o: FridgeEntry) {
                mutex.withLock {
                    db.insertDao()
                        .insert(o)
                    publishRealtime(Insert(o.makeReal()))
                }
            }
        }
    }

    override fun updateDao(): FridgeEntryUpdateDao {
        return object : FridgeEntryUpdateDao {

            override suspend fun update(o: FridgeEntry) {
                mutex.withLock {
                    db.updateDao()
                        .update(o)
                    publishRealtime(Update(o.makeReal()))
                }
            }
        }
    }

    override fun deleteDao(): FridgeEntryDeleteDao {
        return object : FridgeEntryDeleteDao {

            override suspend fun delete(o: FridgeEntry) {
                mutex.withLock {
                    db.deleteDao()
                        .delete(o)
                    publishRealtime(Delete(o.makeReal()))
                }
            }

            override suspend fun deleteAll() {
                mutex.withLock {
                    db.deleteDao()
                        .deleteAll()
                    publishRealtime(DeleteAll)
                }
            }
        }
    }
}
