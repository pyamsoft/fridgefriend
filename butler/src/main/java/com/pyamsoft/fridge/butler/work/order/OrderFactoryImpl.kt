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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.work.Order
import com.pyamsoft.fridge.butler.work.OrderFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class OrderFactoryImpl @Inject internal constructor(
    private val preferences: NotificationPreferences
) : OrderFactory {

    @CheckResult
    override fun itemOrder(params: ItemParameters): Order {
        return ItemOrder(params, preferences)
    }

    override fun locationOrder(params: LocationParameters): Order {
        return LocationOrder(params, preferences)
    }

    @CheckResult
    override fun nightlyOrder(): Order {
        return NightlyOrder()
    }
}