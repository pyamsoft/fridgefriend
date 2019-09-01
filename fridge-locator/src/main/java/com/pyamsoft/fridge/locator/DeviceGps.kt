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

package com.pyamsoft.fridge.locator

import android.app.Activity

interface DeviceGps {

    fun isGpsEnabled(func: (enabled: Boolean) -> Unit)

    fun enableGps(
        activity: Activity,
        onError: (throwable: Throwable) -> Unit
    )

    companion object {

        const val ENABLE_GPS_REQUEST_CODE = 4321
    }
}
