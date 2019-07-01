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
import com.pyamsoft.cachify.cachify
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.PersistentEntries
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Singleton

@Module
object RoomProvider {

  @JvmStatic
  @CheckResult
  private fun provideRoom(context: Context): RoomFridgeDbImpl {
    return Room.databaseBuilder(
        context.applicationContext,
        RoomFridgeDbImpl::class.java,
        "fridge_room_db.db"
    )
        .build()
  }

  @Singleton
  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideDb(context: Context): FridgeDb {
    return provideRoom(context.applicationContext).apply {
      val entryCache = cachify<Sequence<FridgeEntry>, Boolean>(5, MINUTES) { force ->
        return@cachify roomEntryQueryDao()
            .queryAll(force)
            .asSequence()
            .map { JsonMappableFridgeEntry.from(it.makeReal()) }
      }

      val itemCache = cachify<Sequence<FridgeItem>, Boolean>(5, MINUTES) { force ->
        return@cachify roomItemQueryDao()
            .queryAll(force)
            .asSequence()
            .map { JsonMappableFridgeItem.from(it.makeReal()) }
      }
      setObjects(entryCache, itemCache)
    }

  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun providePersistentEntries(impl: RoomPersistentEntries): PersistentEntries {
    return impl
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideFridgeItemDb(room: FridgeDb): FridgeItemDb {
    return room.items()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideItemRealtimeDao(db: FridgeItemDb): FridgeItemRealtime {
    return db.realtime()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideItemQueryDao(db: FridgeItemDb): FridgeItemQueryDao {
    return db.query()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideItemInsertDao(db: FridgeItemDb): FridgeItemInsertDao {
    return db.insert()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideItemUpdateDao(db: FridgeItemDb): FridgeItemUpdateDao {
    return db.update()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideItemDeleteDao(db: FridgeItemDb): FridgeItemDeleteDao {
    return db.delete()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideFridgeEntryDb(room: FridgeDb): FridgeEntryDb {
    return room.entries()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideEntryRealtimeDao(db: FridgeEntryDb): FridgeEntryRealtime {
    return db.realtime()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideEntryQueryDao(db: FridgeEntryDb): FridgeEntryQueryDao {
    return db.query()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideEntryInsertDao(db: FridgeEntryDb): FridgeEntryInsertDao {
    return db.insert()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideEntryUpdateDao(db: FridgeEntryDb): FridgeEntryUpdateDao {
    return db.update()
  }

  @JvmStatic
  @Provides
  @CheckResult
  internal fun provideEntryDeleteDao(db: FridgeEntryDb): FridgeEntryDeleteDao {
    return db.delete()
  }

}
