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

import androidx.annotation.CheckResult
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.pyamsoft.fridge.db.room.converter.DateTypeConverter
import com.pyamsoft.fridge.db.room.converter.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.dao.RoomFridgeItemDeleteDao
import com.pyamsoft.fridge.db.room.dao.RoomFridgeItemInsertDao
import com.pyamsoft.fridge.db.room.dao.RoomFridgeItemQueryDao
import com.pyamsoft.fridge.db.room.dao.RoomFridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

@Database(entities = [RoomFridgeItem::class, RoomFridgeEntry::class], version = 1)
@TypeConverters(PresenceTypeConverter::class, DateTypeConverter::class)
internal abstract class RoomFridgeDb internal constructor() : RoomDatabase(),
  FridgeDb {

  private val realtimeChangeBus = RxBus.create<FridgeItemChangeEvent>()
  private val lock = Any()

  @CheckResult
  internal abstract fun roomQueryDao(): RoomFridgeItemQueryDao

  @CheckResult
  internal abstract fun roomInsertDao(): RoomFridgeItemInsertDao

  @CheckResult
  internal abstract fun roomUpdateDao(): RoomFridgeItemUpdateDao

  @CheckResult
  internal abstract fun roomDeleteDao(): RoomFridgeItemDeleteDao

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
          return roomQueryDao().queryAll()
        }
      }

      override fun queryWithId(id: String): Single<FridgeItem> {
        synchronized(lock) {
          return roomQueryDao().queryWithId(id)
        }
      }

      override fun queryWithEntryId(entryId: String): Single<List<FridgeItem>> {
        synchronized(lock) {
          return roomQueryDao().queryWithEntryId(entryId)
        }
      }

      override fun queryWithName(name: String): Single<List<FridgeItem>> {
        synchronized(lock) {
          return roomQueryDao().queryWithName(name)
        }
      }

      override fun queryWithPresence(presence: Presence): Single<List<FridgeItem>> {
        synchronized(lock) {
          return roomQueryDao().queryWithPresence(presence)
        }
      }

    }
  }

  override fun insert(): FridgeItemInsertDao {
    return object : FridgeItemInsertDao {

      override fun insert(item: FridgeItem): Completable {
        synchronized(lock) {
          return roomInsertDao().insert(item)
            .doOnComplete { publishRealtime(Insert(item)) }
        }
      }

      override fun insertGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return roomInsertDao().insertGroup(items)
            .doOnComplete { publishRealtime(InsertGroup(items)) }
        }
      }

    }
  }

  override fun update(): FridgeItemUpdateDao {
    return object : FridgeItemUpdateDao {

      override fun update(item: FridgeItem): Completable {
        synchronized(lock) {
          return roomUpdateDao().update(item)
            .doOnComplete { publishRealtime(Update(item)) }
        }
      }

      override fun updateGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return roomUpdateDao().updateGroup(items)
            .doOnComplete { publishRealtime(UpdateGroup(items)) }
        }
      }

    }
  }

  override fun delete(): FridgeItemDeleteDao {
    return object : FridgeItemDeleteDao {

      override fun delete(item: FridgeItem): Completable {
        synchronized(lock) {
          return roomDeleteDao().delete(item)
            .doOnComplete { publishRealtime(Delete(item.id())) }
        }
      }

      override fun deleteGroup(items: List<FridgeItem>): Completable {
        synchronized(lock) {
          return roomDeleteDao().deleteGroup(items)
            .doOnComplete { publishRealtime(DeleteGroup(items.map { it.id() })) }
        }
      }

      override fun deleteAll(): Completable {
        synchronized(lock) {
          return roomDeleteDao().deleteAll()
            .doOnComplete { publishRealtime(DeleteAll) }
        }
      }

    }
  }

}