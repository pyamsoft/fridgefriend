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

package com.pyamsoft.fridge.detail.item.add

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItem
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent.Callback
import com.pyamsoft.fridge.detail.item.add.AddNewListItemController.ViewHolder
import javax.inject.Inject

internal class AddNewListItemController internal constructor(
  item: FridgeItem,
  private val builder: DetailItemComponent.Builder,
  private val callback: Callback
) : DetailItem<AddNewListItemController, ViewHolder>(item, swipeable = false),
  Callback {

  override fun getType(): Int {
    return R.id.id_item_add_new_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, builder)
  }

  override fun getLayoutRes(): Int {
    return R.layout.listitem_frame
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind(model, this)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  override fun onAddNewItem() {
    callback.onAddNewItem()
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val builder: DetailItemComponent.Builder
  ) : RecyclerView.ViewHolder(itemView) {

    private var lifecycle: ListItemLifecycle? = null
    @field:Inject internal lateinit var component: AddNewItemUiComponent

    private val parent: ViewGroup = itemView.findViewById(R.id.listitem_frame)

    fun bind(item: FridgeItem, callback: com.pyamsoft.fridge.detail.item.add.AddNewItemUiComponent.Callback) {
      lifecycle?.unbind()

      builder
        .parent(parent)
        .editable(false)
        .item(item)
        .build()
        .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner
      component.bind(owner, null, callback)
      owner.bind()
    }

    fun unbind() {
      lifecycle?.unbind()
      lifecycle = null
    }
  }

  interface Callback {

    fun onAddNewItem()

  }

}