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

import com.popinnow.android.repo.MultiRepo
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber

internal class RoomFridgeItemDb internal constructor(
  private val room: RoomFridgeDbImpl,
  private val repo: MultiRepo<List<FridgeItem>>
) : FridgeItemDb {

  private val realtimeChangeBus = RxBus.create<FridgeItemChangeEvent>()
  private val lock = Any()

  private fun publishRealtime(event: FridgeItemChangeEvent) {
    repo.clear()
    realtimeChangeBus.publish(event)
  }

  override fun realtime(): FridgeItemRealtime {
    return object : FridgeItemRealtime {

      override fun listenForChanges(entryId: String): Observable<FridgeItemChangeEvent> {
        return realtimeChangeBus.listen()
          .filter { it.entryId == entryId }
      }

    }
  }

  override fun query(): FridgeItemQueryDao {
    return object : FridgeItemQueryDao {

      override fun queryAll(force: Boolean, entryId: String): Single<List<FridgeItem>> {
        synchronized(lock) {
          Timber.i("QUERY")
          return repo.get(entryId, force) { room.roomItemQueryDao().queryAll(force, entryId) }
        }
      }

    }
  }

  override fun insert(): FridgeItemInsertDao {
    return object : FridgeItemInsertDao {

      override fun insert(item: FridgeItem): Completable {
        synchronized(lock) {
          Timber.i("INSERT: $item")
          return room.roomItemInsertDao().insert(item)
            .doOnComplete { publishRealtime(Insert(item)) }
        }
      }

    }
  }

  override fun update(): FridgeItemUpdateDao {
    return object : FridgeItemUpdateDao {

      override fun update(item: FridgeItem): Completable {
        synchronized(lock) {
          Timber.i("UPDATE: $item")
          return room.roomItemUpdateDao().update(item)
            .doOnComplete { publishRealtime(Update(item)) }
        }
      }

    }
  }

  override fun delete(): FridgeItemDeleteDao {
    return object : FridgeItemDeleteDao {

      override fun delete(item: FridgeItem): Completable {
        synchronized(lock) {
          Timber.i("DELETE: $item")
          return room.roomItemDeleteDao().delete(item)
            .doOnComplete { publishRealtime(Delete(item)) }
        }
      }

    }
  }

}