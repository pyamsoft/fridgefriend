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

package com.pyamsoft.fridge.db.room.dao.entry

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.room.dao.applyDbSchedulers
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import io.reactivex.Completable
import io.reactivex.Single

@Dao
internal abstract class RoomFridgeEntryUpdateDao internal constructor() :
  FridgeEntryUpdateDao {

  override fun update(entry: FridgeEntry): Completable {
    return Single.just(entry)
      .map { RoomFridgeEntry.create(it) }
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoUpdate(it) }
          .applyDbSchedulers()
      }
  }

  @Update(onConflict = OnConflictStrategy.FAIL)
  internal abstract fun daoUpdate(entry: RoomFridgeEntry)

}