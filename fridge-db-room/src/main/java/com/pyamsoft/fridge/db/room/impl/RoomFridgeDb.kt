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
import com.pyamsoft.fridge.db.FridgeChangeEvent
import com.pyamsoft.fridge.db.FridgeChangeEvent.Delete
import com.pyamsoft.fridge.db.FridgeChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.FridgeChangeEvent.DeleteGroup
import com.pyamsoft.fridge.db.FridgeChangeEvent.Insert
import com.pyamsoft.fridge.db.FridgeChangeEvent.Update
import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import com.pyamsoft.fridge.db.FridgeDbInsertDao
import com.pyamsoft.fridge.db.FridgeDbQueryDao
import com.pyamsoft.fridge.db.FridgeDbRealtime
import com.pyamsoft.fridge.db.FridgeDbUpdateDao
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.FridgeItem.Presence
import com.pyamsoft.fridge.db.room.converters.DateTypeConverter
import com.pyamsoft.fridge.db.room.converters.PresenceTypeConverter
import com.pyamsoft.fridge.db.room.dao.RoomDeleteDao
import com.pyamsoft.fridge.db.room.dao.RoomInsertDao
import com.pyamsoft.fridge.db.room.dao.RoomQueryDao
import com.pyamsoft.fridge.db.room.dao.RoomUpdateDao
import com.pyamsoft.pydroid.core.bus.RxBus
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

@Database(entities = [RoomFridgeItem::class], version = 1)
@TypeConverters(PresenceTypeConverter::class, DateTypeConverter::class)
internal abstract class RoomFridgeDb internal constructor() : RoomDatabase(),
  FridgeDb {

  private val realtimeChangeBus = RxBus.create<FridgeChangeEvent>()
  private val lock = Any()

  @CheckResult
  internal abstract fun roomQueryDao(): RoomQueryDao

  @CheckResult
  internal abstract fun roomInsertDao(): RoomInsertDao

  @CheckResult
  internal abstract fun roomUpdateDao(): RoomUpdateDao

  @CheckResult
  internal abstract fun roomDeleteDao(): RoomDeleteDao

  private fun publishRealtime(event: FridgeChangeEvent) {
    realtimeChangeBus.publish(event)
  }

  override fun realtime(): FridgeDbRealtime {
    return object : FridgeDbRealtime {

      override fun listenForChanges(): Observable<FridgeChangeEvent> {
        return realtimeChangeBus.listen()
      }

    }
  }

  override fun query(): FridgeDbQueryDao {
    return object : FridgeDbQueryDao {

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

  override fun insert(): FridgeDbInsertDao {
    return object : FridgeDbInsertDao {

      override fun insert(item: FridgeItem): Completable {
        synchronized(lock) {
          return roomInsertDao().insert(item)
            .doOnComplete { publishRealtime(Insert(item)) }
        }
      }

    }
  }

  override fun update(): FridgeDbUpdateDao {
    return object : FridgeDbUpdateDao {

      override fun update(item: FridgeItem): Completable {
        synchronized(lock) {
          return roomUpdateDao().update(item)
            .doOnComplete { publishRealtime(Update(item)) }
        }
      }

    }
  }

  override fun delete(): FridgeDbDeleteDao {
    return object : FridgeDbDeleteDao {

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