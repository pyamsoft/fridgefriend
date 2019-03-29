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

package com.pyamsoft.fridge.detail.list.item.fridge

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.list.item.DetailItem
import com.pyamsoft.fridge.detail.list.item.DetailItemComponent
import com.pyamsoft.fridge.detail.list.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemController.ViewHolder
import javax.inject.Inject

internal class DetailListItemController internal constructor(
  item: FridgeItem,
  private val builder: DetailItemComponent.Builder,
  private val callback: DetailListItemController.Callback
) : DetailItem<DetailListItemController, ViewHolder>(item),
  DetailListItemUiComponent.Callback {

  override fun getType(): Int {
    return R.id.id_item_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, builder)
  }

  override fun getLayoutRes(): Int {
    return R.layout.listitem_linear_horizontal
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind(model, this)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  override fun onNonRealItemDelete(item: FridgeItem) {
    callback.onDelete(item)
  }

  override fun onUpdateItemError(throwable: Throwable) {
    callback.onCommitError(throwable)
  }

  override fun onDeleteItemError(throwable: Throwable) {
    callback.onDeleteError(throwable)
  }

  override fun onModelUpdate(item: FridgeItem) {
    withModel(item)
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val builder: DetailItemComponent.Builder
  ) : RecyclerView.ViewHolder(itemView) {

    private var lifecycle: ListItemLifecycle? = null
    @field:Inject internal lateinit var component: DetailListItemUiComponent

    private val parent: ViewGroup = itemView.findViewById(R.id.listitem_linear_h)

    fun bind(item: FridgeItem, callback: DetailListItemUiComponent.Callback) {
      lifecycle?.unbind()

      builder
        .parent(parent)
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

    fun onDelete(item: FridgeItem)

    fun onDeleteError(throwable: Throwable)

    fun onCommitError(throwable: Throwable)

  }

}