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
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.pyamsoft.fridge.db.FridgeDbUpdateDao
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.db.room.impl.RoomFridgeItem
import com.pyamsoft.fridge.db.room.impl.applyDbSchedulers
import io.reactivex.Completable
import io.reactivex.Single

@Dao
internal abstract class RoomUpdateDao internal constructor() : FridgeDbUpdateDao {

  override fun update(item: FridgeItem): Completable {
    return Single.just(item)
      .map { RoomFridgeItem.create(it) }
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoUpdate(it) }
          .applyDbSchedulers()
      }
  }

  @Update(onConflict = OnConflictStrategy.FAIL)
  internal abstract fun daoUpdate(item: RoomFridgeItem)

}