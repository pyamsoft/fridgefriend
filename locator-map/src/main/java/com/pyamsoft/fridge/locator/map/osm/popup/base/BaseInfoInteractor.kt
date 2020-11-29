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

package com.pyamsoft.fridge.locator.map.osm.popup.base

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.injector.LocationInjector
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.runner.WorkResult
import com.pyamsoft.fridge.db.BaseDb
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID

internal abstract class BaseInfoInteractor<
        T : Any,
        RE : Any,
        R : BaseDb.Realtime<RE>,
        Q : BaseDb.Query<T>,
        I : BaseDb.Insert<T>,
        D : BaseDb.Delete<T>
        > protected constructor(
    private val locationInjector: LocationInjector,
    private val realtime: R,
    private val queryDao: Q,
    private val insertDao: I,
    private val deleteDao: D
) {

    @CheckResult
    suspend fun getAllCached(): List<T> = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        queryDao.query(false)
    }

    protected suspend inline fun listenChanges(
        crossinline onEvent: (event: RE) -> Unit
    ) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        realtime.listenForChanges { onEvent(it) }
    }

    suspend fun deleteFromDb(data: T) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        if (deleteDao.delete(data)) {
            Timber.d("Favorite removed: $data")
            fireLocationQuery()
        }
    }

    suspend fun insertIntoDb(data: T) = withContext(context = Dispatchers.Default) {
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

        val id = UUID.randomUUID()
        val tag = "Location Reminder 1"

        when (val result = locationInjector.execute(
            id, setOf(tag), LocationParameters(forceNotifyNeeded = true)
        )) {
            is WorkResult.Success -> Timber.d("Location reminder success: ${result.id}")
            is WorkResult.Cancel -> Timber.w("Location reminder cancelled: ${result.id}")
            is WorkResult.Failure -> Timber.e("Location reminder error: ${result.id}")
        }
    }


}

