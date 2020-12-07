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

package com.pyamsoft.fridge.locator.map.osm.popup

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.locator.map.popup.store.StoreInfoInteractor
import javax.inject.Inject

internal class StoreInfoInteractorImpl @Inject internal constructor(
    butler: Butler,
    orderFactory: OrderFactory,
    realtime: NearbyStoreRealtime,
    queryDao: NearbyStoreQueryDao,
    insertDao: NearbyStoreInsertDao,
    deleteDao: NearbyStoreDeleteDao
) : StoreInfoInteractor, BaseInfoInteractorImpl<
        NearbyStore,
        NearbyStoreChangeEvent,
        NearbyStoreRealtime,
        NearbyStoreQueryDao,
        NearbyStoreInsertDao,
        NearbyStoreDeleteDao
        >(butler, orderFactory, realtime, queryDao, insertDao, deleteDao) {

    override fun onRealtimeChange(
        event: NearbyStoreChangeEvent,
        onInsert: (NearbyStore) -> Unit,
        onDelete: (NearbyStore) -> Unit
    ) {
        return when (event) {
            is NearbyStoreChangeEvent.Insert -> onInsert(event.store)
            is NearbyStoreChangeEvent.Delete -> onDelete(event.store)
            is NearbyStoreChangeEvent.Update -> {
                // Ignore Update events
                Unit
            }
        }
    }
}
