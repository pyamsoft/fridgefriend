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

package com.pyamsoft.fridge.butler.injector

import android.content.Context
import com.pyamsoft.fridge.butler.order.OrderFactory
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.runner.ItemRunner
import com.pyamsoft.fridge.butler.runner.WorkResult
import com.pyamsoft.pydroid.ui.Injector
import java.util.UUID
import javax.inject.Inject

class ItemInjector(context: Context) : BaseInjector<ItemParameters>(context) {

    @JvmField
    @Inject
    internal var runner: ItemRunner? = null

    // TODO(Peter) For some reason this is null if we inject it in BaseInjector
    @JvmField
    @Inject
    internal var orderFactory: OrderFactory? = null

    override suspend fun onExecute(
        context: Context,
        id: UUID,
        tags: Set<String>,
        params: ItemParameters
    ): WorkResult {
        Injector.obtain<ButlerComponent>(context.applicationContext).inject(this)

        return requireNotNull(runner).doWork(id, tags, params) {
            requireNotNull(orderFactory).itemOrder(
                ItemParameters(
                    forceNotifyExpiring = false,
                    forceNotifyNeeded = false
                )
            )
        }
    }
}
