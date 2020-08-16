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

package com.pyamsoft.fridge.db

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategoryDb
import com.pyamsoft.fridge.db.category.FridgeCategoryDeleteDao
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.category.FridgeCategoryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.guarantee.EntryGuarantee
import com.pyamsoft.fridge.db.guarantee.EntryGuaranteeImpl
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.persist.PersistentCategories
import com.pyamsoft.fridge.db.persist.PersistentCategoriesImpl
import com.pyamsoft.fridge.db.persist.PersistentEntries
import com.pyamsoft.fridge.db.persist.PersistentEntriesImpl
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.db.zone.NearbyZoneDb
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
private annotation class InternalApi

@Module
abstract class DbModule {

    @Binds
    @CheckResult
    internal abstract fun providePersistentEntries(impl: PersistentEntriesImpl): PersistentEntries

    @Binds
    @CheckResult
    internal abstract fun providePersistentCategories(impl: PersistentCategoriesImpl): PersistentCategories

    @Binds
    @CheckResult
    internal abstract fun provideEntryGuarantee(impl: EntryGuaranteeImpl): EntryGuarantee

    @Module
    companion object {

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        internal fun provideItemDb(db: FridgeDb): FridgeItemDb {
            return db.items()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemRealtimeDao(@InternalApi db: FridgeItemDb): FridgeItemRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemQueryDao(@InternalApi db: FridgeItemDb): FridgeItemQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemInsertDao(@InternalApi db: FridgeItemDb): FridgeItemInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemDeleteDao(@InternalApi db: FridgeItemDb): FridgeItemDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        internal fun provideEntryDb(db: FridgeDb): FridgeEntryDb {
            return db.entries()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryRealtimeDao(@InternalApi db: FridgeEntryDb): FridgeEntryRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryQueryDao(@InternalApi db: FridgeEntryDb): FridgeEntryQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryInsertDao(@InternalApi db: FridgeEntryDb): FridgeEntryInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryDeleteDao(@InternalApi db: FridgeEntryDb): FridgeEntryDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        internal fun provideStoreDb(db: FridgeDb): NearbyStoreDb {
            return db.stores()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideStoreRealtimeDao(@InternalApi db: NearbyStoreDb): NearbyStoreRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideStoreQueryDao(@InternalApi db: NearbyStoreDb): NearbyStoreQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideStoreInsertDao(@InternalApi db: NearbyStoreDb): NearbyStoreInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideStoreDeleteDao(@InternalApi db: NearbyStoreDb): NearbyStoreDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        internal fun provideZoneDb(db: FridgeDb): NearbyZoneDb {
            return db.zones()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideZoneRealtimeDao(@InternalApi db: NearbyZoneDb): NearbyZoneRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideZoneQueryDao(@InternalApi db: NearbyZoneDb): NearbyZoneQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideZoneInsertDao(@InternalApi db: NearbyZoneDb): NearbyZoneInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideZoneDeleteDao(@InternalApi db: NearbyZoneDb): NearbyZoneDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        @InternalApi
        internal fun provideCategoryDb(db: FridgeDb): FridgeCategoryDb {
            return db.categories()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideCategoryRealtimeDao(@InternalApi db: FridgeCategoryDb): FridgeCategoryRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideCategoryQueryDao(@InternalApi db: FridgeCategoryDb): FridgeCategoryQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideCategoryInsertDao(@InternalApi db: FridgeCategoryDb): FridgeCategoryInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideCategoryDeleteDao(@InternalApi db: FridgeCategoryDb): FridgeCategoryDeleteDao {
            return db.deleteDao()
        }
    }
}
