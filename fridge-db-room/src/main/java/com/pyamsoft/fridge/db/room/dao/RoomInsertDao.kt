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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.fridge.db.FridgeDbInsertDao
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.room.impl.RoomFridgeItem
import com.pyamsoft.fridge.db.room.impl.applyDbSchedulers
import io.reactivex.Completable
import io.reactivex.Single

@Dao
internal abstract class RoomInsertDao internal constructor() : FridgeDbInsertDao {

  override fun insert(item: FridgeItem): Completable {
    return Single.just(item)
      .map { RoomFridgeItem.create(item) }
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoInsert(it) }
          .applyDbSchedulers()
      }
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  internal abstract fun daoInsert(item: RoomFridgeItem)

}