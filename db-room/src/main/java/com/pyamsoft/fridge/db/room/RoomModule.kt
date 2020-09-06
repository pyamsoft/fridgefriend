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
import com.pyamsoft.cachify.Cached
import com.pyamsoft.cachify.MemoryCacheStorage
import com.pyamsoft.cachify.MultiCached1
import com.pyamsoft.cachify.MultiCached2
import com.pyamsoft.cachify.cachify
import com.pyamsoft.cachify.multiCachify
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.JsonMappableFridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.db.store.JsonMappableNearbyStore
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.JsonMappableNearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone
import dagger.Module
import dagger.Provides
import java.util.Locale
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

        private const val SIMILARITY_MIN_SCORE_CUTOFF = 0.45F
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
        internal fun provideEntryCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeEntry>> {
            return cachify<List<FridgeEntry>>(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) {
                db.roomEntryQueryDao()
                    .query(false)
                    .map { JsonMappableFridgeEntry.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideAllItemsCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeItem>> {
            return cachify<List<FridgeItem>>(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) {
                db.roomItemQueryDao()
                    .query(false)
                    .map { JsonMappableFridgeItem.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideItemsByEntryCache(@InternalApi db: RoomFridgeDb): MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id> {
            return multiCachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { id ->
                db.roomItemQueryDao()
                    .query(false, id)
                    .map { JsonMappableFridgeItem.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideSameNameDifferentPresenceCache(@InternalApi db: RoomFridgeDb): MultiCached2<FridgeItemDb.QuerySameNameDifferentPresenceKey, List<FridgeItem>, String, FridgeItem.Presence> {
            return multiCachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { name, presence ->
                db.roomItemQueryDao()
                    .querySameNameDifferentPresence(false, name, presence)
                    .map { JsonMappableFridgeItem.from(it.makeReal()) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideSimilarlyNamedCache(@InternalApi db: RoomFridgeDb): MultiCached2<FridgeItemDb.QuerySimilarNamedKey, List<FridgeItemDb.SimilarityScore>, FridgeItem.Id, String> {
            return multiCachify(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) { id, name ->
                db.roomItemQueryDao()
                    .querySimilarNamedItems(false, id, name)

                    // Do this step in Kotlin because I don't know how to do this distance algo in SQL
                    .asSequence()
                    .map { JsonMappableFridgeItem.from(it.makeReal()) }
                    .map { fridgeItem ->
                        val itemName = fridgeItem.name().toLowerCase(Locale.getDefault()).trim()

                        val score = when {
                            itemName == name -> 1.0F
                            itemName.startsWith(name) -> 0.75F
                            itemName.endsWith(name) -> 0.5F
                            else -> itemName.withDistanceRatio(name)
                        }
                        return@map FridgeItemDb.SimilarityScore(fridgeItem, score)
                    }
                    .filterNot { it.score < SIMILARITY_MIN_SCORE_CUTOFF }
                    .sortedBy { it.score }
                    .toList()
            }
        }

        @CheckResult
        private fun String.withDistanceRatio(str: String): Float {
            // Initialize a zero-matrix
            val s1Len = this.length
            val s2Len = str.length
            val rows = s1Len + 1
            val columns = s2Len + 1
            val matrix = Array(rows) { IntArray(columns) { 0 } }

            // Populate matrix with indices of each character in strings
            for (i in 1 until rows) {
                matrix[i][0] = i
            }

            for (j in 1 until columns) {
                matrix[0][j] = j
            }

            // Calculate the cost of deletes, inserts, and subs
            for (col in 1 until columns) {
                for (row in 1 until rows) {
                    // If the character is the same in a given position, cost is 0, else cost is 2
                    val cost = if (this[row - 1] == str[col - 1]) 0 else 2

                    // The cost of a deletion, insertion, and substitution
                    val deleteCost = matrix[row - 1][col] + 1
                    val insertCost = matrix[row][col - 1] + 1
                    val substitutionCost = matrix[row - 1][col - 1] + cost

                    // Populate the matrix
                    matrix[row][col] =
                        kotlin.math.min(deleteCost, kotlin.math.min(insertCost, substitutionCost))
                }
            }

            // Calculate distance ratio
            val totalLength = (s1Len + s2Len)
            return (totalLength - matrix[s1Len][s2Len]).toFloat() / totalLength
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideZoneCache(@InternalApi db: RoomFridgeDb): Cached<List<NearbyZone>> {
            return cachify<List<NearbyZone>>(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) {
                db.roomZoneQueryDao()
                    .query(false)
                    .map { JsonMappableNearbyZone.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideStoreCache(@InternalApi db: RoomFridgeDb): Cached<List<NearbyStore>> {
            return cachify<List<NearbyStore>>(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) {
                db.roomStoreQueryDao()
                    .query(false)
                    .map { JsonMappableNearbyStore.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @CheckResult
        @InternalApi
        internal fun provideCategoryCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeCategory>> {
            return cachify<List<FridgeCategory>>(
                storage = MemoryCacheStorage.create(cacheTime, cacheUnit)
            ) {
                db.roomCategoryQueryDao()
                    .query(false)
                    .map { JsonMappableFridgeCategory.from(it) }
            }
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideDb(
            @InternalApi db: RoomFridgeDb,

            // Items
            @InternalApi allItemsCache: Cached<List<FridgeItem>>,
            @InternalApi itemsByEntryCache: MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id>,
            @InternalApi sameNameDifferentPresenceCache: MultiCached2<FridgeItemDb.QuerySameNameDifferentPresenceKey, List<FridgeItem>, String, FridgeItem.Presence>,
            @InternalApi similarNamedCache: MultiCached2<FridgeItemDb.QuerySimilarNamedKey, List<FridgeItemDb.SimilarityScore>, FridgeItem.Id, String>,

            // Entries
            @InternalApi entryCache: Cached<List<FridgeEntry>>,

            // Stores
            @InternalApi storeCache: Cached<List<NearbyStore>>,

            // Zones
            @InternalApi zoneCache: Cached<List<NearbyZone>>,

            // Categories
            @InternalApi categoryCache: Cached<List<FridgeCategory>>
        ): FridgeDb {

            return FridgeDbImpl(
                db,

                // Items
                allItemsCache,
                itemsByEntryCache,
                sameNameDifferentPresenceCache,
                similarNamedCache,

                // Entries
                entryCache,

                // Stores
                storeCache,

                // Zones
                zoneCache,

                // Categories
                categoryCache
            )
        }
    }
}
