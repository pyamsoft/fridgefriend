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
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.runner.BaseRunner
import com.pyamsoft.fridge.butler.runner.WorkResult
import com.pyamsoft.fridge.butler.runner.locator.LocationRunner
import com.pyamsoft.pydroid.ui.Injector
import javax.inject.Inject

class LocationInjector(context: Context) : BaseInjector<LocationParameters>(context) {

    @JvmField
    @Inject
    internal var delegate: LocationRunner? = null

    override suspend fun onRun(context: Context, params: LocationParameters): WorkResult {
        Injector.obtain<ButlerComponent>(context).inject(this)
        val result = requireNotNull(delegate).doWork(params)
        delegate = null
        return result
    }
}
