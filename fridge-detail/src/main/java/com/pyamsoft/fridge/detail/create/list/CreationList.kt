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

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.DetailList
import com.pyamsoft.fridge.detail.item.DetailItem
import com.pyamsoft.fridge.detail.item.DetailItemComponent.Builder
import com.pyamsoft.fridge.detail.item.add.AddNewListItemController
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemController
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject
import javax.inject.Named

internal class CreationList @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  interactor: CreationListInteractor,
  imageLoader: ImageLoader,
  stateMap: MutableMap<String, Int>,
  theming: Theming,
  fakeRealtime: EventBus<FridgeItemChangeEvent>,
  parent: ViewGroup,
  private val listCallback: CreationList.Callback
) : DetailList(interactor, imageLoader, stateMap, theming, fakeRealtime, parent, listCallback),
  AddNewListItemController.Callback {

  override fun createListItem(item: FridgeItem, builder: Builder): DetailItem<*, *> {
    if (item.id().isBlank()) {
      return AddNewListItemController(
        item,
        builder,
        this
      )
    } else {
      return DetailListItemController(
        item,
        true,
        builder
      )
    }
  }

  override fun onAddNewItem() {
    listCallback.onAddNewItem()
  }

  interface Callback : DetailList.Callback {

    fun onAddNewItem()
  }

}
