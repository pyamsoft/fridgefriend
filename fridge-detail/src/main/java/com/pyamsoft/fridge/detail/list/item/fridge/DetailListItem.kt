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
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.list.item.DetailItem
import com.pyamsoft.fridge.detail.list.item.DetailItemUiComponentFactory
import com.pyamsoft.fridge.detail.list.item.ListItemLifecycle
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItem.ViewHolder
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemUiComponent.Callback

internal class DetailListItem internal constructor(
  item: FridgeItem,
  private val factory: DetailItemUiComponentFactory,
  private val callback: Callback
) : DetailItem<DetailListItem, ViewHolder>(item),
  Callback {

  override fun getType(): Int {
    return R.id.id_item_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, factory)
  }

  override fun getLayoutRes(): Int {
    return R.layout.detail_list_item
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
    private val factory: DetailItemUiComponentFactory
  ) : RecyclerView.ViewHolder(itemView) {

    private var lifecycle: ListItemLifecycle? = null

    fun bind(item: FridgeItem, callback: com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemUiComponent.Callback) {
      lifecycle?.unbind()

      val owner = ListItemLifecycle()
      lifecycle = owner

      val component = factory.createItem(itemView, item)
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