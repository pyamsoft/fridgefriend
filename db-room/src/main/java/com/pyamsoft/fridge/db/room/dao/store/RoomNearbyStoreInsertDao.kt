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

package com.pyamsoft.fridge.db.room.dao.store

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.fridge.db.room.entity.RoomNearbyStore
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Dao
internal abstract class RoomNearbyStoreInsertDao internal constructor() : NearbyStoreInsertDao {

    override suspend fun insert(o: NearbyStore) = withContext(context = Dispatchers.IO) {
        val roomNearbyStore = RoomNearbyStore.create(o)
        daoInsert(roomNearbyStore)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    internal abstract fun daoInsert(entry: RoomNearbyStore)
}
