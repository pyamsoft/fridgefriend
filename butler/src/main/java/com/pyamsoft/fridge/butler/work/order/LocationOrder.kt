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

package com.pyamsoft.fridge.butler.work.order

import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.work.OrderParameters

class LocationOrder internal constructor(
    private val params: LocationParameters,
    preferences: NotificationPreferences
) : NotifyingOrder(preferences) {

    override suspend fun tag(): String {
        return "Location Reminder 1"
    }

    override suspend fun parameters(): OrderParameters {
        return OrderParameters {
            putBoolean(FORCE_LOCATION_NOTIFICATION, params.forceNotifyNearby)
        }
    }

    companion object {

        const val FORCE_LOCATION_NOTIFICATION = "force_location_notifications_v1"
    }
}

