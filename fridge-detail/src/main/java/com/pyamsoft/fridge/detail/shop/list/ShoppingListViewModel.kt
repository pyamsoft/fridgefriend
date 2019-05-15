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

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.list.DetailListViewModel
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject

class ShoppingListViewModel @Inject internal constructor(
  private val interactor: ShoppingListInteractor,
  fakeRealtime: EventBus<FridgeItemChangeEvent>
) : DetailListViewModel(fakeRealtime, filterArchived = false) {

  override fun getItems(force: Boolean): Single<List<FridgeItem>> {
    return interactor.getItems(force)
  }

  override fun listenForChanges(): Observable<FridgeItemChangeEvent> {
    return interactor.listenForChanges()
  }

  override fun getListItems(items: List<FridgeItem>): List<FridgeItem> {
    return items.filter { it.isReal() }
  }

}
