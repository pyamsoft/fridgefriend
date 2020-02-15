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
import androidx.work.Data
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.injector.BaseInjector
import com.pyamsoft.fridge.butler.injector.GeofenceNotifyInjector

internal class GeofenceNotifierWorker internal constructor(
    context: Context,
    params: WorkerParameters
) : BaseWorker(context.applicationContext, params) {

    override fun getInjector(context: Context, data: Data): BaseInjector {
        val fenceIds = data.getStringArray(KEY_FENCES) ?: emptyArray()
        val latitude = data.getDouble(KEY_CURRENT_LATITUDE, BAD_COORDINATE)
        val currentLat = if (latitude == BAD_COORDINATE) null else latitude
        val longitude = data.getDouble(KEY_CURRENT_LONGITUDE, BAD_COORDINATE)
        val currentLon = if (longitude == BAD_COORDINATE) null else longitude
        return GeofenceNotifyInjector(context.applicationContext, fenceIds, currentLat, currentLon)
    }

    companion object {

        private const val BAD_COORDINATE = 42069.69420

        internal const val KEY_FENCES = "key_fences"
        internal const val KEY_CURRENT_LATITUDE = "key_current_latitude"
        internal const val KEY_CURRENT_LONGITUDE = "key_current_longitude"
    }
}
