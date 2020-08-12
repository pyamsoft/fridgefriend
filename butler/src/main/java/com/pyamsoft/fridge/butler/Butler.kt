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

package com.pyamsoft.fridge.butler

import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.params.LocationParameters

interface Butler {

    suspend fun remindItems(params: ItemParameters)

    suspend fun scheduleRemindItems(params: ItemParameters)

    suspend fun cancelItemsReminder()

    suspend fun remindLocation(params: LocationParameters)

    suspend fun scheduleRemindLocation(params: LocationParameters)

    suspend fun cancelLocationReminder()

    suspend fun scheduleRemindNightly(params: EmptyParameters)

    suspend fun cancelNightlyReminder()

    suspend fun cancel()
}
