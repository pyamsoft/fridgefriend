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

package com.pyamsoft.fridge.entry.action

import com.pyamsoft.fridge.entry.action.EntryActionHandler.ActionEvent
import com.pyamsoft.fridge.entry.action.EntryActionHandler.ActionEvent.Create
import com.pyamsoft.fridge.entry.action.EntryActionHandler.ActionEvent.Shop
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class EntryActionHandler @Inject internal constructor(
  bus: EventBus<ActionEvent>
) : UiEventHandler<ActionEvent, EntryActionCallback>(bus),
  EntryActionCallback {

  override fun onCreateClicked() {
    publish(Create)
  }

  override fun onShopClicked() {
    publish(Shop)
  }

  override fun handle(delegate: EntryActionCallback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        return@subscribe when (it) {
          is Create -> delegate.onCreateClicked()
          is Shop -> delegate.onShopClicked()
        }
      }
  }

  sealed class ActionEvent {
    object Create : ActionEvent()
    object Shop : ActionEvent()
  }

}
