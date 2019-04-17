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

package com.pyamsoft.fridge.detail.item.fridge

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent.CommitDate
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent.CommitName
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.fridge.DetailItemHandler.DetailItemEvent.LastDone
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class DetailItemHandler @Inject internal constructor(
  bus: EventBus<DetailItemEvent>
) : UiEventHandler<DetailItemEvent, DetailItemCallback>(bus),
  DetailItemCallback {

  override fun commitName(oldItem: FridgeItem, name: String) {
    publish(CommitName(oldItem, name))
  }

  override fun onLastDoneClicked() {
    publish(LastDone)
  }

  override fun commitDate(oldItem: FridgeItem, year: Int, month: Int, day: Int) {
    publish(CommitDate(oldItem, year, month, day))
  }

  override fun commitPresence(oldItem: FridgeItem, presence: Presence) {
    publish(CommitPresence(oldItem, presence))
  }

  override fun handle(delegate: DetailItemCallback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(Schedulers.io())
      .subscribe {
        return@subscribe when (it) {
          is CommitName -> delegate.commitName(it.oldItem, it.name)
          is CommitDate -> delegate.commitDate(it.oldItem, it.year, it.month, it.day)
          is CommitPresence -> delegate.commitPresence(it.oldItem, it.presence)
          is LastDone -> delegate.onLastDoneClicked()
        }
      }
  }

  sealed class DetailItemEvent {
    data class CommitName(val oldItem: FridgeItem, val name: String) : DetailItemEvent()

    data class CommitDate(val oldItem: FridgeItem, val year: Int, val month: Int, val day: Int) :
      DetailItemEvent()

    data class CommitPresence(val oldItem: FridgeItem, val presence: Presence) : DetailItemEvent()

    object LastDone : DetailItemEvent()
  }
}