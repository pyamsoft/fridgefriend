/*
 * Copyright 2021 Peter Kenji Yamanaka
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

import com.pyamsoft.fridge.butler.work.Order
import com.pyamsoft.fridge.butler.work.OrderParameters
import com.pyamsoft.fridge.core.today
import java.util.Calendar

class NightlyOrder internal constructor() : Order {

    override suspend fun tag(): String {
        return "Nightly Reminder 1"
    }

    override suspend fun parameters(): OrderParameters {
        return OrderParameters.empty()
    }

    override suspend fun period(): Long {
        // Get the time now
        val now = today { set(Calendar.MILLISECOND, 0) }

        // Get the time in the evening at our notification hour
        val evening = today {
            set(Calendar.HOUR_OF_DAY, 8 + 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If it is after this evening, schedule for tomorrow evening
        if (now.after(evening)) {
            evening.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Calculate the difference
        val nowMillis = now.timeInMillis
        val eveningMillis = evening.timeInMillis
        return eveningMillis - nowMillis
    }

}

