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

import com.pyamsoft.fridge.preference.NotificationPreferences
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.work.OrderParameters

class ItemOrder internal constructor(
    private val params: ItemParameters,
    preferences: NotificationPreferences
) : NotifyingOrder(preferences) {

    override suspend fun tag(): String {
        return "Items Reminder 1"
    }

    override suspend fun parameters(): OrderParameters {
        return OrderParameters {
            putBoolean(FORCE_EXPIRING_NOTIFICATION, params.forceNotifyExpiring)
            putBoolean(FORCE_NEEDED_NOTIFICATION, params.forceNotifyNeeded)
        }
    }

    companion object {

        const val FORCE_NEEDED_NOTIFICATION = "force_needed_notifications_v1"
        const val FORCE_EXPIRING_NOTIFICATION = "force_expiring_notifications_v1"
    }

}

