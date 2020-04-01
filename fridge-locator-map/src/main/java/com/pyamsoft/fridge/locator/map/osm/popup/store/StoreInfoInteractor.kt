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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoInteractor
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject

internal class StoreInfoInteractor @Inject internal constructor(
    enforcer: Enforcer,
    butler: Butler,
    realtime: NearbyStoreRealtime,
    queryDao: NearbyStoreQueryDao,
    insertDao: NearbyStoreInsertDao,
    deleteDao: NearbyStoreDeleteDao
) : BaseInfoInteractor<
    NearbyStore,
    NearbyStoreChangeEvent,
    NearbyStoreRealtime,
    NearbyStoreQueryDao,
    NearbyStoreInsertDao,
    NearbyStoreDeleteDao
    >(enforcer, butler, realtime, queryDao, insertDao, deleteDao) {

    suspend inline fun listenForNearbyCacheChanges(
        crossinline onInsert: (store: NearbyStore) -> Unit,
        crossinline onDelete: (store: NearbyStore) -> Unit
    ) = listenChanges { event ->
        return@listenChanges when (event) {
            is NearbyStoreChangeEvent.Insert -> onInsert(event.store)
            is NearbyStoreChangeEvent.Delete -> onDelete(event.store)
            is NearbyStoreChangeEvent.Update -> {
                // Ignore Update events
                Unit
            }
        }
    }
}
