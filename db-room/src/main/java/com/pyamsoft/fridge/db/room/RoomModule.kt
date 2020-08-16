/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.db.room

import android.content.Context
import androidx.annotation.CheckResult
import androidx.room.Room
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.cachify.MemoryCacheStorage
import com.pyamsoft.cachify.cachify
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.JsonMappableFridgeCategory
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
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
private annotation class InternalApi

@Module
abstract class RoomModule {

    @Module
    companion object {

        private const val cacheTime = 10L
        private val cacheUnit = MINUTES

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideRoom(context: Context): RoomFridgeDb {
            return Room.databaseBuilder(
                context.applicationContext,
                RoomFridgeDbImpl::class.java,
                "fridge_room_db.db"
            )
                .build()
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideEntryCache(@InternalApi db: RoomFridgeDb): Cached1<Sequence<FridgeEntry>, Boolean> {
            return cachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { force ->
                db.roomEntryQueryDao()
                    .query(force)
                    .asSequence()
                    .map { JsonMappableFridgeEntry.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideItemCache(@InternalApi db: RoomFridgeDb): Cached1<Sequence<FridgeItem>, Boolean> {
            return cachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { force ->
                db.roomItemQueryDao()
                    .query(force)
                    .asSequence()
                    .map { JsonMappableFridgeItem.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideZoneCache(@InternalApi db: RoomFridgeDb): Cached1<Sequence<NearbyZone>, Boolean> {
            return cachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { force ->
                db.roomZoneQueryDao()
                    .query(force)
                    .asSequence()
                    .map { JsonMappableNearbyZone.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideStoreCache(@InternalApi db: RoomFridgeDb): Cached1<Sequence<NearbyStore>, Boolean> {
            return cachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { force ->
                db.roomStoreQueryDao()
                    .query(force)
                    .asSequence()
                    .map { JsonMappableNearbyStore.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideCategoryCache(@InternalApi db: RoomFridgeDb): Cached1<Sequence<FridgeCategory>, Boolean> {
            return cachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { force ->
                db.roomCategoryQueryDao()
                    .query(force)
                    .asSequence()
                    .map { JsonMappableFridgeCategory.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideDb(
            @InternalApi db: RoomFridgeDb,
            @InternalApi entryCache: Cached1<Sequence<FridgeEntry>, Boolean>,
            @InternalApi itemCache: Cached1<Sequence<FridgeItem>, Boolean>,
            @InternalApi storeCache: Cached1<Sequence<NearbyStore>, Boolean>,
            @InternalApi zoneCache: Cached1<Sequence<NearbyZone>, Boolean>,
            @InternalApi categoryCache: Cached1<Sequence<FridgeCategory>, Boolean>
        ): FridgeDb {

            return FridgeDbImpl(
                db,
                entryCache,
                itemCache,
                storeCache,
                zoneCache,
                categoryCache
            )
        }
    }
}
