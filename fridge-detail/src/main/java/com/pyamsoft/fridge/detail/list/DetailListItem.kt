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

import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.items.ModelAbstractItem
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.R
import java.util.Date

internal class DetailListItem internal constructor(
  item: FridgeItem,
  private val entryId: String,
  private val callback: DetailListItem.Callback
) : ModelAbstractItem<FridgeItem, DetailListItem, DetailListItem.ViewHolder>(item) {

  override fun getType(): Int {
    return R.id.id_item_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, entryId, callback)
  }

  override fun getLayoutRes(): Int {
    return R.layout.detail_list_item
  }

  override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
    super.bindView(holder, payloads)
    holder.bind(model)
  }

  override fun unbindView(holder: ViewHolder) {
    super.unbindView(holder)
    holder.unbind()
  }

  class ViewHolder internal constructor(
    itemView: View,
    private val entryId: String,
    private val callback: DetailListItem.Callback
  ) : RecyclerView.ViewHolder(itemView) {

    private val itemName = itemView.findViewById<EditText>(R.id.detail_item_name)

    private var itemId: String = ""

    fun bind(item: FridgeItem) {
      itemId = item.id()

      itemName.setTextKeepState(item.name())
    }

    fun unbind() {
      if (itemId.isNotBlank()) {
        val name = itemName.text.toString().trim()
        val expireTime = Date()
        val presence = Presence.NEED
        callback.onCommit(
          FridgeItem.create(itemId, entryId, name, expireTime, presence),
          finalUpdate = true
        )
      }
      itemName.text.clear()

      itemId = ""
    }

  }

  interface Callback {

    fun onCommit(item: FridgeItem, finalUpdate: Boolean)

  }

}