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
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.impl.FridgeDb
import com.pyamsoft.fridge.db.room.impl.FridgeDbImpl
import com.pyamsoft.fridge.db.room.impl.RoomFridgeDb
import dagger.Module
import dagger.Provides
import javax.inject.Named
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
  @Named("room-realtime")
  internal fun provideRealtimeDao(db: FridgeDb): FridgeItemRealtime {
    return db.realtime()
  }

  @JvmStatic
  @Provides
  @CheckResult
  @Named("room-query-dao")
  internal fun provideQueryDao(db: FridgeDb): FridgeItemQueryDao {
    return db.query()
  }

  @JvmStatic
  @Provides
  @CheckResult
  @Named("room-insert-dao")
  internal fun provideInsertDao(db: FridgeDb): FridgeItemInsertDao {
    return db.insert()
  }

  @JvmStatic
  @Provides
  @CheckResult
  @Named("room-update-dao")
  internal fun provideUpdateDao(db: FridgeDb): FridgeItemUpdateDao {
    return db.update()
  }

  @JvmStatic
  @Provides
  @CheckResult
  @Named("room-delete-dao")
  internal fun provideDeleteDao(db: FridgeDb): FridgeItemDeleteDao {
    return db.delete()
  }

}