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

package com.pyamsoft.fridge.detail.create.list

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.create.list.CreationListHandler.CreationEvent
import com.pyamsoft.fridge.detail.create.list.CreationListHandler.CreationEvent.Expand
import com.pyamsoft.fridge.detail.create.list.CreationListHandler.CreationEvent.Refresh
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.fridge.detail.list.DetailListHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import javax.inject.Inject

internal class CreationListHandler @Inject internal constructor(
  bus: EventBus<CreationEvent>
) : DetailListHandler<CreationEvent, DetailList.Callback>(bus), DetailList.Callback {

  override fun onRefresh() {
    publish(Refresh)
  }

  override fun onExpandItem(item: FridgeItem) {
    publish(Expand(item))
  }

  override fun handleEvent(
    event: CreationEvent,
    delegate: DetailList.Callback
  ) {
    return when (event) {
      is Refresh -> delegate.onRefresh()
      is Expand -> delegate.onExpandItem(event.item)
    }
  }

  sealed class CreationEvent : ListEvent {
    object Refresh : CreationEvent()

    data class Expand(val item: FridgeItem) : CreationEvent()
  }
}
