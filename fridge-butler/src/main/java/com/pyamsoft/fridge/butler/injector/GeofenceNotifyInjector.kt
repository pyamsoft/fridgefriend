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

package com.pyamsoft.fridge.butler.injector

import android.content.Context
import com.pyamsoft.fridge.butler.injector.component.InputButlerComponent
import com.pyamsoft.fridge.butler.runner.WorkResult
import com.pyamsoft.fridge.butler.runner.geofence.GeofenceNotifierRunner
import com.pyamsoft.pydroid.ui.Injector
import javax.inject.Inject

class GeofenceNotifyInjector(
    context: Context,
    private val fenceIds: List<String>,
    private val latitude: Double?,
    private val longitude: Double?
) : BaseInjector(context) {

    @JvmField
    @Inject
    internal var delegate: GeofenceNotifierRunner? = null

    override suspend fun onRun(context: Context): WorkResult {
        Injector.obtain<InputButlerComponent.Factory>(context).create(fenceIds, latitude, longitude)
            .inject(this)
        val result = requireNotNull(delegate).doWork()
        delegate = null
        return result
    }
}
