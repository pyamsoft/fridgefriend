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

package com.pyamsoft.fridge.entry.list

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

internal class EntryListItem internal constructor(
  entry: FridgeEntry,
  private val callback: EntryListItem.Callback
) : EntryItem<EntryListItem, EntryListItem.ViewHolder>(entry) {

  override fun getType(): Int {
    return R.id.id_entry_list_item
  }

  override fun getViewHolder(v: View): ViewHolder {
    return ViewHolder(v, callback)
  }

  override fun getLayoutRes(): Int {
    return R.layout.entry_list_item
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
    private val callback: EntryListItem.Callback
  ) : RecyclerView.ViewHolder(itemView) {

    private val entryName = itemView.findViewById<TextView>(R.id.entry_item_name)

    fun bind(entry: FridgeEntry) {
      itemView.setOnDebouncedClickListener { callback.onItemClicked(entry) }
      entryName.text = entry.name()
    }

    fun unbind() {
      itemView.setOnDebouncedClickListener(null)
      entryName.text = ""
    }

  }

  interface Callback {

    fun onItemClicked(entry: FridgeEntry)

  }

}