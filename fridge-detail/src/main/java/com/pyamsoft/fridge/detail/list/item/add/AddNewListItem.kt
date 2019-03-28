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

package com.pyamsoft.fridge.detail.list.item.add

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.list.item.DetailItem
import com.pyamsoft.fridge.detail.list.item.DetailItemUiComponentFactory
import com.pyamsoft.fridge.detail.list.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.list.item.add.AddNewListItem.ViewHolder

internal class AddNewListItem internal constructor(
  item: FridgeItem,
  private val factory: DetailItemUiComponentFactory,
  private val callback: Callback
) : DetailItem<AddNewListItem, ViewHolder>(item) {

  override fun getType(): Int {
    return R.id.id_item_add_new_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, factory, callback)
  }

  override fun getLayoutRes(): Int {
    return R.layout.add_new_list_item
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind()
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val factory: DetailItemUiComponentFactory,
    private val callback: Callback
  ) : RecyclerView.ViewHolder(itemView),
    com.pyamsoft.fridge.detail.list.item.add.AddNewItemUiComponent.Callback {

    private var lifecycle: ListItemLifecycle? = null

    fun bind() {
      lifecycle?.unbind()

      val owner = ListItemLifecycle()
      lifecycle = owner

      val component = factory.createAddNewItem(itemView)
      component.bind(owner, null, this)
      owner.bind()
    }

    fun unbind() {
      lifecycle?.unbind()
      lifecycle = null
    }

    override fun onAddNewItem() {
      callback.onAddNewItem()
    }
  }

  interface Callback {

    fun onAddNewItem()

  }

}