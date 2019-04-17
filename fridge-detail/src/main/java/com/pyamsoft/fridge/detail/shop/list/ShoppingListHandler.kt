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

package com.pyamsoft.fridge.detail.shop.list

import com.pyamsoft.fridge.detail.list.DetailListHandler
import com.pyamsoft.fridge.detail.shop.list.ShoppingList.Callback
import com.pyamsoft.fridge.detail.shop.list.ShoppingListHandler.ShoppingEvent
import com.pyamsoft.fridge.detail.shop.list.ShoppingListHandler.ShoppingEvent.Refresh
import com.pyamsoft.pydroid.core.bus.EventBus
import javax.inject.Inject

internal class ShoppingListHandler @Inject internal constructor(
  bus: EventBus<ShoppingEvent>
) : DetailListHandler<ShoppingEvent, Callback>(bus), Callback {

  override fun onRefresh() {
    publish(Refresh)
  }

  override fun handleEvent(event: ShoppingEvent, delegate: Callback) {
    return when (event) {
      is Refresh -> delegate.onRefresh()
    }
  }

  sealed class ShoppingEvent : ListEvent {
    object Refresh : ShoppingEvent()
  }
}