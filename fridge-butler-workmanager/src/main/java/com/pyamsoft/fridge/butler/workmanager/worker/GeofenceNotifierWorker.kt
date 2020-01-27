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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.injector.GeofenceNotifyInjector
import com.pyamsoft.fridge.butler.runner.WorkResult

internal class GeofenceNotifierWorker internal constructor(
    context: Context,
    params: WorkerParameters
) :
    CoroutineWorker(context.applicationContext, params) {

    override suspend fun doWork(): Result {
        val fenceIds = inputData.getStringArray(KEY_FENCES) ?: emptyArray()
        val latitude = inputData.getDouble(KEY_CURRENT_LATITUDE, BAD_COORDINATE)
        val currentLat = if (latitude == BAD_COORDINATE) null else latitude
        val longitude = inputData.getDouble(KEY_CURRENT_LONGITUDE, BAD_COORDINATE)
        val currentLon = if (longitude == BAD_COORDINATE) null else longitude
        val injector = GeofenceNotifyInjector(applicationContext, fenceIds, currentLat, currentLon)
        return if (injector.run() === WorkResult.Success) Result.success() else Result.failure()
    }

    companion object {

        private const val BAD_COORDINATE = 42069.69420

        internal const val KEY_FENCES = "key_fences"
        internal const val KEY_CURRENT_LATITUDE = "key_current_latitude"
        internal const val KEY_CURRENT_LONGITUDE = "key_current_longitude"
    }
}
