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
import com.pyamsoft.cachify.MemoryCacheStorage
import com.pyamsoft.cachify.cachify
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.db.store.JsonMappableNearbyStore
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.JsonMappableNearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Singleton

@Module
abstract class RoomModule {

    @Module
    companion object {

        @JvmStatic
        @CheckResult
        private fun provideRoom(context: Context): RoomFridgeDbImpl {
            return Room.databaseBuilder(
                context.applicationContext,
                RoomFridgeDbImpl::class.java,
                "fridge_room_db.db"
            ).fallbackToDestructiveMigration()
                .build()
        }

        @Singleton
        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideDb(context: Context): FridgeDb {
            return provideRoom(context.applicationContext).apply {
                val entryCache =
                    cachify<Sequence<FridgeEntry>, Boolean>(
                        storage = MemoryCacheStorage.create(5, MINUTES)
                    ) { force ->
                        return@cachify roomEntryQueryDao()
                            .query(force)
                            .asSequence()
                            .map { JsonMappableFridgeEntry.from(it.makeReal()) }
                    }

                val itemCache = cachify<Sequence<FridgeItem>, Boolean>(
                    storage = MemoryCacheStorage.create(5, MINUTES)
                ) { force ->
                    return@cachify roomItemQueryDao()
                        .query(force)
                        .asSequence()
                        .map { JsonMappableFridgeItem.from(it.makeReal()) }
                }

                val storeCache =
                    cachify<Sequence<NearbyStore>, Boolean>(
                        storage = MemoryCacheStorage.create(5, MINUTES)
                    ) { force ->
                        return@cachify roomStoreQueryDao()
                            .query(force)
                            .asSequence()
                            .map { JsonMappableNearbyStore.from(it) }
                    }

                val zoneCache = cachify<Sequence<NearbyZone>, Boolean>(
                    storage = MemoryCacheStorage.create(5, MINUTES)
                ) { force ->
                    return@cachify roomZoneQueryDao()
                        .query(force)
                        .asSequence()
                        .map { JsonMappableNearbyZone.from(it) }
                }

                applyCaches(entryCache, itemCache, storeCache, zoneCache)
            }
        }
    }
}
