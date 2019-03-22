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

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.DeleteGroup
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.InsertGroup
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.UpdateGroup
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal class RoomFridgeItemDb internal constructor(
  private val room: RoomFridgeDbImpl
) : FridgeItemDb {

  private val realtimeChangeBus = RxBus.create<FridgeItemChangeEvent>()
  private val lock = Any()

  private fun publishRealtime(event: FridgeItemChangeEvent) {
    realtimeChangeBus.publish(event)
  }

  override fun realtime(): FridgeItemRealtime {
    return object : FridgeItemRealtime {

      override fun listenForChanges(): Observable<FridgeItemChangeEvent> {
        return realtimeChangeBus.listen()
      }

    }
  }

  override fun query(): FridgeItemQueryDao {
    return object : FridgeItemQueryDao {

      override fun queryAll(): Single<List<FridgeItem>> {
        synchronized(lock) {
          return room.roomItemQueryDao().queryAll()
        }
      }

      override fun queryWithId(id: String): Single<FridgeItem> {
        synchronized(lock) {
          return room.roomItemQueryDao().queryWithId(id)
        }
      }

      override fun queryWithEntryId(entryId: String): Single<List<FridgeItem>> {
        synchronized(lock) {
          return room.roomItemQueryDao().queryWithEntryId(entryId)
        }
      }

      override fun queryWithName(name: String): Single<List<FridgeItem>> {
        synchronized(lock) {
          return room.roomItemQueryDao().queryWithName(name)
        }
      }

      override fun queryWithPresence(presence: Presence): Single<List<FridgeItem>> {
        synchronized(lock) {
          return room.roomItemQueryDao().queryWithPresence(presence)
        }
      }

    }
  }

  override fun insert(): FridgeItemInsertDao {
    return object : FridgeItemInsertDao {

      override fun insert(item: FridgeItem): Completable {
        synchronized(lock) {
          return room.roomItemInsertDao().insert(item)
            .doOnComplete { publishRealtime(Insert(item)) }
        }
      }

      override fun insertGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return room.roomItemInsertDao().insertGroup(items)
            .doOnComplete { publishRealtime(InsertGroup(items)) }
        }
      }

    }
  }

  override fun update(): FridgeItemUpdateDao {
    return object : FridgeItemUpdateDao {

      override fun update(item: FridgeItem): Completable {
        synchronized(lock) {
          return room.roomItemUpdateDao().update(item)
            .doOnComplete { publishRealtime(Update(item)) }
        }
      }

      override fun updateGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return room.roomItemUpdateDao().updateGroup(items)
            .doOnComplete { publishRealtime(UpdateGroup(items)) }
        }
      }

    }
  }

  override fun delete(): FridgeItemDeleteDao {
    return object : FridgeItemDeleteDao {

      override fun delete(item: FridgeItem): Completable {
        synchronized(lock) {
          return room.roomItemDeleteDao().delete(item)
            .doOnComplete { publishRealtime(Delete(item.id())) }
        }
      }

      override fun deleteGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return room.roomItemDeleteDao().deleteGroup(items)
            .doOnComplete { publishRealtime(DeleteGroup(items.map { it.id() })) }
        }
      }

      override fun deleteAll(): Completable {
        synchronized(lock) {
          return room.roomItemDeleteDao().deleteAll()
            .doOnComplete { publishRealtime(DeleteAll) }
        }
      }

    }
  }

}