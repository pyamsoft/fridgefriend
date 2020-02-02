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
 *
 */

package com.pyamsoft.fridge.db

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.db.entry.FridgeEntryUpdateDao
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.persist.PersistentEntries
import com.pyamsoft.fridge.db.persist.PersistentEntriesImpl
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.db.store.NearbyStoreUpdateDao
import com.pyamsoft.fridge.db.zone.NearbyZoneDb
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import com.pyamsoft.fridge.db.zone.NearbyZoneUpdateDao
import dagger.Binds
import dagger.Module
import dagger.Provides

@Module
abstract class DbModule {

    @Binds
    @CheckResult
    internal abstract fun providePersistentEntries(impl: PersistentEntriesImpl): PersistentEntries

    @Module
    companion object {

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideFridgeItemDb(db: FridgeDb): FridgeItemDb {
            return db.items()
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
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemInsertDao(db: FridgeItemDb): FridgeItemInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemUpdateDao(db: FridgeItemDb): FridgeItemUpdateDao {
            return db.updateDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideItemDeleteDao(db: FridgeItemDb): FridgeItemDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideFridgeEntryDb(db: FridgeDb): FridgeEntryDb {
            return db.entries()
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
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryInsertDao(db: FridgeEntryDb): FridgeEntryInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryUpdateDao(db: FridgeEntryDb): FridgeEntryUpdateDao {
            return db.updateDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideEntryDeleteDao(db: FridgeEntryDb): FridgeEntryDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreDb(db: FridgeDb): NearbyStoreDb {
            return db.stores()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreRealtimeDao(db: NearbyStoreDb): NearbyStoreRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreQueryDao(db: NearbyStoreDb): NearbyStoreQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreInsertDao(db: NearbyStoreDb): NearbyStoreInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreUpdateDao(db: NearbyStoreDb): NearbyStoreUpdateDao {
            return db.updateDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyStoreDeleteDao(db: NearbyStoreDb): NearbyStoreDeleteDao {
            return db.deleteDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneDb(db: FridgeDb): NearbyZoneDb {
            return db.zones()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneRealtimeDao(db: NearbyZoneDb): NearbyZoneRealtime {
            return db.realtime()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneQueryDao(db: NearbyZoneDb): NearbyZoneQueryDao {
            return db.queryDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneInsertDao(db: NearbyZoneDb): NearbyZoneInsertDao {
            return db.insertDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneUpdateDao(db: NearbyZoneDb): NearbyZoneUpdateDao {
            return db.updateDao()
        }

        @JvmStatic
        @Provides
        @CheckResult
        internal fun provideNearbyZoneDeleteDao(db: NearbyZoneDb): NearbyZoneDeleteDao {
            return db.deleteDao()
        }
    }
}
