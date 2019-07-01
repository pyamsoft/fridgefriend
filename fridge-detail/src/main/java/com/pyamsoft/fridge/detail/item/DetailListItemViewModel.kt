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

package com.pyamsoft.fridge.detail.item

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ArchiveItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CloseItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.DeleteItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.PickDate
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.tryCancel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class DetailListItemViewModel @Inject internal constructor(
  @Named("item_editable") isEditable: Boolean,
  fakeRealtime: EventBus<FridgeItemChangeEvent>,
  private val interactor: DetailInteractor,
  private val item: FridgeItem
) : DetailItemViewModel(isEditable, item, fakeRealtime) {

  override fun onInit() {
  }

  override fun handleViewEvent(event: DetailItemViewEvent) {
    return when (event) {
      is CommitPresence -> commitPresence(event.oldItem, event.presence)
      is ExpandItem -> expandItem(event.item)
      is CommitName, is PickDate, is CloseItem, is DeleteItem, is ArchiveItem -> {
        Timber.d("Ignore event: $event")
      }
    }
  }

  private fun commitPresence(
    oldItem: FridgeItem,
    presence: Presence
  ) {
    // Stop any pending updates
    updateJob.tryCancel()

    if (oldItem.isReal()) {
      commitItem(item = oldItem.presence(presence))
    }
  }

  private fun commitItem(item: FridgeItem) {
    // A delete operation will stop an update operation
    updateJob = viewModelScope.launch {
      try {
        withContext(context = Dispatchers.Default) { interactor.commit(item.makeReal()) }
      } catch (e: Throwable) {
        if (e !is CancellationException) {
          Timber.e(e, "Error updating item: ${item.id()}")
        }
      } finally {

      }
    }
  }

  fun archive() {
    remove(item, doRemove = { interactor.archive(it) })
  }

  fun delete() {
    remove(item, doRemove = { interactor.delete(it) })
  }

  private fun expandItem(item: FridgeItem) {
    publish(ExpandDetails(item))
  }

}
