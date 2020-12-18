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
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.db.BaseDb
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoInteractor
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class BaseInfoInteractorImpl<
        T : Any,
        RE : Any,
        R : BaseDb.Realtime<RE>,
        Q : BaseDb.Query<T>,
        I : BaseDb.Insert<T>,
        D : BaseDb.Delete<T>
        > protected constructor(
    private val butler: Butler,
    private val orderFactory: OrderFactory,
    private val realtime: R,
    private val queryDao: Q,
    private val insertDao: I,
    private val deleteDao: D
) : BaseInfoInteractor<T> {

    override suspend fun getAllCached(): List<T> = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        queryDao.query(false)
    }

    override suspend fun listenForNearbyCacheChanges(onInsert: (T) -> Unit, onDelete: (T) -> Unit) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            realtime.listenForChanges { onRealtimeChange(it, onInsert, onDelete) }
        }

    override suspend fun deleteFromDb(data: T, offerUndo: Boolean) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        if (deleteDao.delete(data, offerUndo)) {
            Timber.d("Favorite removed: $data")
            fireLocationQuery()
        }
    }

    override suspend fun insertIntoDb(data: T) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        if (insertDao.insert(data)) {
            Timber.d("Favorite inserted: $data")
        } else {
            Timber.d("Favorite updated: $data")
        }
        fireLocationQuery()
    }

    private suspend fun fireLocationQuery() {
        Enforcer.assertOffMainThread()
        val order = orderFactory.locationOrder(LocationParameters(forceNotifyNearby = true))
        butler.placeOrder(order)
    }

    protected abstract fun onRealtimeChange(event: RE, onInsert: (T) -> Unit, onDelete: (T) -> Unit)

}

