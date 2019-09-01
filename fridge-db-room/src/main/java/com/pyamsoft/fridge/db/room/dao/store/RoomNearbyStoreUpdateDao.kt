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

package com.pyamsoft.fridge.db.room.dao.store

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.pyamsoft.fridge.db.room.entity.RoomNearbyStore
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreUpdateDao
import timber.log.Timber

@Dao
internal abstract class RoomNearbyStoreUpdateDao internal constructor() : NearbyStoreUpdateDao {

    override suspend fun update(o: NearbyStore) {
        Timber.d("ROOM: NearbyStore Update: $o")
        val roomNearbyStore = RoomNearbyStore.create(o)
        daoUpdate(roomNearbyStore)
    }

    @Update(onConflict = OnConflictStrategy.ABORT)
    internal abstract fun daoUpdate(entry: RoomNearbyStore)
}
