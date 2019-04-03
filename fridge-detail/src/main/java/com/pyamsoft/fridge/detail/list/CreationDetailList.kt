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

package com.pyamsoft.fridge.detail.list

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.item.DetailItem
import com.pyamsoft.fridge.detail.list.item.DetailItemComponent.Builder
import com.pyamsoft.fridge.detail.list.item.add.AddNewListItemController
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemController
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject
import javax.inject.Named

internal class CreationDetailList @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  interactor: DetailListInteractor,
  imageLoader: ImageLoader,
  stateMap: MutableMap<String, Int>,
  theming: Theming,
  parent: ViewGroup,
  callback: DetailList.Callback
) : DetailList(interactor, imageLoader, stateMap, theming, parent, callback),
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
        builder,
        this
      )
    }
  }

  override fun onListEmpty() {
    addNewItem()
  }

  override fun onAddNewItem() {
    addNewItem()
  }

  private fun addNewItem() {
    insert(FridgeItem.create(entryId = entryId))
  }
}
