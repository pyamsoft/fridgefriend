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

package com.pyamsoft.fridge.setting.toolbar

import com.pyamsoft.fridge.setting.toolbar.SettingToolbar.Callback
import com.pyamsoft.fridge.setting.toolbar.SettingToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.setting.toolbar.SettingToolbarHandler.ToolbarEvent.Navigate
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class SettingToolbarHandler @Inject internal constructor(
  bus: EventBus<ToolbarEvent>
) : UiEventHandler<ToolbarEvent, Callback>(bus),
  Callback {

  override fun onNavigateClicked() {
    publish(Navigate)
  }

  override fun handle(delegate: Callback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe {
        return@subscribe when (it) {
          is Navigate -> delegate.onNavigateClicked()
        }
      }
  }

  sealed class ToolbarEvent {
    object Navigate : ToolbarEvent()
  }

}
