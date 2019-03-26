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

package com.pyamsoft.fridge.db.room.impl

import com.popinnow.android.repo.Repo
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Delete
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

internal class RoomFridgeEntryDb internal constructor(
  private val room: RoomFridgeDbImpl,
  private val repo: Repo<List<JsonMappableFridgeEntry>>
) : FridgeEntryDb {

  private val realtimeChangeBus = RxBus.create<FridgeEntryChangeEvent>()
  private val lock = Any()

  private fun publishRealtime(event: FridgeEntryChangeEvent) {
    repo.clear()
    realtimeChangeBus.publish(event)
  }

  override fun realtime(): FridgeEntryRealtime {
    return object : FridgeEntryRealtime {

      override fun listenForChanges(): Observable<FridgeEntryChangeEvent> {
        return realtimeChangeBus.listen()
      }

    }
  }

  override fun query(): FridgeEntryQueryDao {
    return object : FridgeEntryQueryDao {

      override fun queryAll(force: Boolean): Single<List<FridgeEntry>> {
        synchronized(lock) {
          Timber.i("QUERY")
          return repo.get(force) {
            return@get room.roomEntryQueryDao().queryAll(force)
              .map { it.map { entry -> JsonMappableFridgeEntry.from(entry) } }
          }.map { it }
        }
      }

    }
  }

  override fun insert(): FridgeEntryInsertDao {
    return object : FridgeEntryInsertDao {

      override fun insert(entry: FridgeEntry): Completable {
        synchronized(lock) {
          Timber.i("INSERT: $entry")
          return room.roomEntryInsertDao().insert(entry)
            .doOnComplete { publishRealtime(Insert(entry)) }
        }
      }

    }
  }

  override fun update(): FridgeEntryUpdateDao {
    return object : FridgeEntryUpdateDao {

      override fun update(entry: FridgeEntry): Completable {
        synchronized(lock) {
          Timber.i("UPDATE: $entry")
          return room.roomEntryUpdateDao().update(entry)
            .doOnComplete { publishRealtime(Update(entry)) }
        }
      }

    }
  }

  override fun delete(): FridgeEntryDeleteDao {
    return object : FridgeEntryDeleteDao {

      override fun delete(entry: FridgeEntry): Completable {
        synchronized(lock) {
          Timber.i("DELETE: $entry")
          return room.roomEntryDeleteDao().delete(entry)
            .doOnComplete { publishRealtime(Delete(entry.id())) }
        }
      }

      override fun deleteAll(): Completable {
        synchronized(lock) {
          Timber.i("DELETE: ALL")
          return room.roomEntryDeleteDao().deleteAll()
            .doOnComplete { publishRealtime(DeleteAll) }
        }
      }

    }
  }

}