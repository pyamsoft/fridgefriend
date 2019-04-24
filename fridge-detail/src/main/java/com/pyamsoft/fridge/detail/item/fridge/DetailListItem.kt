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

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.BaseUiView

internal abstract class DetailListItem<C : DetailListItem.Callback> protected constructor(
  item: FridgeItem,
  parent: ViewGroup,
  callback: C
) : BaseUiView<C>(parent, callback) {

  protected var item: FridgeItem = item
    private set

  fun updateItem(real: FridgeItem) {
    this.item = real
    onItemUpdated()
  }

  protected abstract fun onItemUpdated()

  interface Callback

}

