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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailListPresenter
import com.pyamsoft.fridge.detail.create.CreationScope
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@CreationScope
internal class CreationListPresenter @Inject internal constructor(
  private val interactor: CreationListInteractor,
  @Named("detail_entry_id") private val entryId: String,
  fakeRealtime: EventBus<FridgeItemChangeEvent>
) : DetailListPresenter(fakeRealtime),
  CreationList.Callback {

  override fun getItems(force: Boolean): Single<List<FridgeItem>> {
    return interactor.getItems(entryId, force)
  }

  override fun listenForChanges(): Observable<FridgeItemChangeEvent> {
    return interactor.listenForChanges(entryId)
  }

  override fun getListItems(items: List<FridgeItem>): List<FridgeItem> {
    val mutableItems = items.toMutableList()
    if (mutableItems.filterNot { it.id().isBlank() }.isEmpty()) {
      addNewItem(mutableItems)
    }
    insert(mutableItems, FridgeItem.empty())

    return mutableItems.sortedWith(Comparator { o1, o2 ->
      return@Comparator when {
        o1.id().isBlank() -> 1
        o2.id().isBlank() -> -1
        else -> 0
      }
    })
  }

  override fun onAddNewItem() {
    fakeRealtime.publish(FridgeItemChangeEvent.Insert(createNewItem()))
  }

  private fun addNewItem(items: MutableList<FridgeItem>) {
    insert(items, createNewItem())
  }

  @CheckResult
  private fun createNewItem(): FridgeItem {
    return FridgeItem.create(entryId = entryId)
  }

}
