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

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Delete
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class FridgeEntryDbImpl internal constructor(
  private val db: FridgeEntryDb,
  private val cache: Cached1<Sequence<FridgeEntry>, Boolean>,
  private val onCacheCleared: () -> Unit
) : FridgeEntryDb {

  private val mutex = Mutex()

  private fun publishRealtime(event: FridgeEntryChangeEvent) {
    cache.clear()
    onCacheCleared()
    publish(event)
  }

  override fun publish(event: FridgeEntryChangeEvent) {
    db.publish(event)
  }

  override fun realtime(): FridgeEntryRealtime {
    return db.realtime()
  }

  override fun query(): FridgeEntryQueryDao {
    return object : FridgeEntryQueryDao {

      override suspend fun queryAll(force: Boolean): List<FridgeEntry> {
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

  override fun insert(): FridgeEntryInsertDao {
    return object : FridgeEntryInsertDao {

      override suspend fun insert(entry: FridgeEntry) {
        mutex.withLock {
          db.insert()
              .insert(entry)
          publishRealtime(Insert(entry.makeReal()))
        }
      }

    }
  }

  override fun update(): FridgeEntryUpdateDao {
    return object : FridgeEntryUpdateDao {

      override suspend fun update(entry: FridgeEntry) {
        mutex.withLock {
          db.update()
              .update(entry)
          publishRealtime(Update(entry.makeReal()))
        }
      }
    }

  }

  override fun delete(): FridgeEntryDeleteDao {
    return object : FridgeEntryDeleteDao {

      override suspend fun delete(entry: FridgeEntry) {
        mutex.withLock {
          db.delete()
              .delete(entry)
          publishRealtime(Delete(entry.makeReal()))
        }
      }

      override suspend fun deleteAll() {
        mutex.withLock {
          db.delete()
              .deleteAll()
          publishRealtime(DeleteAll)
        }
      }

    }
  }

}
