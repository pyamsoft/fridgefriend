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

package com.pyamsoft.fridge.detail.item.add

import com.pyamsoft.fridge.detail.item.add.AddNewItemHandler.AddNewEvent
import com.pyamsoft.fridge.detail.item.add.AddNewItemHandler.AddNewEvent.Add
import com.pyamsoft.fridge.detail.item.add.AddNewItemView.Callback
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class AddNewItemHandler @Inject internal constructor(
  bus: EventBus<AddNewEvent>
) : UiEventHandler<AddNewEvent, Callback>(bus), Callback {

  override fun onAddNewClicked() {
    publish(Add)
  }

  override fun handle(delegate: Callback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe {
        return@subscribe when (it) {
          is Add -> delegate.onAddNewClicked()
        }
      }
  }

  sealed class AddNewEvent {
    object Add : AddNewEvent()
  }
}