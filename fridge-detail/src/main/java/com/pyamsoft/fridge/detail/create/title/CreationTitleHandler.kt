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

package com.pyamsoft.fridge.detail.create.title

import com.pyamsoft.fridge.detail.create.title.CreationTitleHandler.TitleEvent
import com.pyamsoft.fridge.detail.create.title.CreationTitleHandler.TitleEvent.Update
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class CreationTitleHandler @Inject internal constructor(
  bus: EventBus<TitleEvent>
) : UiEventHandler<TitleEvent, CreationTitle.Callback>(bus),
  CreationTitle.Callback {

  override fun onUpdateName(name: String) {
    publish(Update(name))
  }

  override fun handle(delegate: CreationTitle.Callback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe {
        return@subscribe when (it) {
          is Update -> delegate.onUpdateName(it.name)
        }
      }
  }

  sealed class TitleEvent {
    data class Update(val name: String) : TitleEvent()
  }

}
