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

package com.pyamsoft.fridge.db.room

import android.content.Context
import androidx.annotation.CheckResult
import androidx.room.Room
import com.pyamsoft.fridge.db.FridgeDbDeleteDao
import com.pyamsoft.fridge.db.FridgeDbInsertDao
import com.pyamsoft.fridge.db.FridgeDbQueryDao
import com.pyamsoft.fridge.db.FridgeDbRealtime
import com.pyamsoft.fridge.db.FridgeDbUpdateDao
import com.pyamsoft.fridge.db.room.impl.FridgeDb
import com.pyamsoft.fridge.db.room.impl.FridgeDbImpl
import com.pyamsoft.fridge.db.room.impl.RoomFridgeDb
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object RoomProvider {

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDb(context: Context): FridgeDb {
    val room = Room.databaseBuilder(
      context.applicationContext,
      RoomFridgeDb::class.java,
      "fridge_item_room_db.db"
    )
      .build()

    return FridgeDbImpl(room)
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideRealtimeDao(db: FridgeDb): FridgeDbRealtime {
    return db.realtime()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideQueryDao(db: FridgeDb): FridgeDbQueryDao {
    return db.query()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideInsertDao(db: FridgeDb): FridgeDbInsertDao {
    return db.insert()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideUpdateDao(db: FridgeDb): FridgeDbUpdateDao {
    return db.update()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDeleteDao(db: FridgeDb): FridgeDbDeleteDao {
    return db.delete()
  }

}