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
import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import io.reactivex.Completable

@Dao
internal abstract class RoomDeleteDao internal constructor() : FridgeDbDeleteDao {

  override fun deleteAll(): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun deleteWithId(id: String): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun deleteWithName(name: String): Completable {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}