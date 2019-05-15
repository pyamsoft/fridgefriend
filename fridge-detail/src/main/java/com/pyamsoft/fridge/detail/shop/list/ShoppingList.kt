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

package com.pyamsoft.fridge.detail.shop.list

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DaggerDetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import javax.inject.Inject

class ShoppingList @Inject internal constructor(
  parent: ViewGroup,
  owner: LifecycleOwner,
  private val interactor: CreationListInteractor,
  private val imageLoader: ImageLoader,
  private val theming: Theming,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : DetailList(parent, owner, editable = false) {

  override fun createDaggerComponent(
    parent: ViewGroup,
    item: FridgeItem,
    editable: Boolean
  ): DetailItemComponent {
    return DaggerDetailItemComponent.factory()
        .create(parent, item, editable, imageLoader, theming, interactor, fakeRealtime)
  }

}
