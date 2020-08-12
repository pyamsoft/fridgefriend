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

package com.pyamsoft.fridge

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.params.LocationParameters

suspend fun Butler.initOnAppStart(params: ButlerParameters) {
    cancel()

    remindItems(
        ItemParameters(
            forceNotifyNeeded = params.forceNotifyNeeded,
            forceNotifyExpiring = params.forceNotifyExpiring
        )
    )

    remindLocation(
        LocationParameters(
            forceNotifyNeeded = params.forceNotifyNeeded
        )
    )

    // Nightly notifications are always scheduled since they must fire at an exact time.
    scheduleRemindNightly(EmptyParameters)
}

data class ButlerParameters(val forceNotifyNeeded: Boolean, val forceNotifyExpiring: Boolean)
