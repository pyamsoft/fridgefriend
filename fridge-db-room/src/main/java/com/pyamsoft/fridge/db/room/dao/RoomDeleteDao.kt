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

package com.pyamsoft.fridge.db.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.room.impl.RoomFridgeItem
import com.pyamsoft.fridge.db.room.impl.applyDbSchedulers
import io.reactivex.Completable
import io.reactivex.Single

@Dao
internal abstract class RoomDeleteDao internal constructor() : FridgeDbDeleteDao {

  override fun delete(item: FridgeItem): Completable {
    return Single.just(item)
      .map { RoomFridgeItem.create(it) }
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoDelete(it) }
          .applyDbSchedulers()
      }
  }

  @Delete
  internal abstract fun daoDelete(item: RoomFridgeItem)

  override fun deleteGroup(items: List<FridgeItem>): Completable {
    return Single.just(items)
      .flatMapCompletable {
        val roomItems = items.map { RoomFridgeItem.create(it) }
        return@flatMapCompletable Completable.fromAction { daoDeleteGroup(roomItems) }
          .applyDbSchedulers()
      }
  }

  @Delete
  internal abstract fun daoDeleteGroup(items: List<RoomFridgeItem>)

  override fun deleteAll(): Completable {
    return Completable.fromAction { daoDeleteAll() }
  }

  @Query("DELETE FROM ${RoomFridgeItem.TABLE_NAME} WHERE 1 = 1")
  internal abstract fun daoDeleteAll()

}