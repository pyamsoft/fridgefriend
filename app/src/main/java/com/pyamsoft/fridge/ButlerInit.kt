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

import android.app.Activity
import android.app.Application
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.work.OrderFactory

suspend fun Activity.initOnAppStart(
    butler: Butler,
    orderFactory: OrderFactory,
) {
    butler.initOnAppStart(isForeground = true, orderFactory)
}

suspend fun Application.initOnAppStart(
    butler: Butler,
    orderFactory: OrderFactory,
) {
    butler.initOnAppStart(isForeground = false, orderFactory)
}

private suspend fun Butler.initOnAppStart(
    isForeground: Boolean,
    orderFactory: OrderFactory
) {
    cancel()

    // Fire instantly an item order
    placeOrder(
        orderFactory.itemOrder(
            ItemParameters(
                forceNotifyNeeded = true,
                forceNotifyExpiring = true,
            )
        )
    )

    // Nightly notifications are always scheduled since they must fire at an exact time.
    scheduleOrder(orderFactory.nightlyOrder())

    // No location in background
    if (!isForeground) {
        return
    }

    placeOrder(
        orderFactory.locationOrder(
            LocationParameters(
                forceNotifyNearby = true,
            )
        )
    )
}
