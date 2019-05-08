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

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import timber.log.Timber

internal class EntryListAdapter internal constructor(
  private val callback: Callback
) : ListAdapter<FridgeEntry, EntryListAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<FridgeEntry>() {

      override fun areItemsTheSame(
        oldItem: FridgeEntry,
        newItem: FridgeEntry
      ): Boolean {
        return oldItem.id() == newItem.id()
      }

      override fun areContentsTheSame(
        oldItem: FridgeEntry,
        newItem: FridgeEntry
      ): Boolean {
        return JsonMappableFridgeEntry.from(oldItem) == JsonMappableFridgeEntry.from(newItem)
      }

    }
) {

  override fun getItemViewType(position: Int): Int {
    return if (position == 0) R.id.id_entry_space_item else R.id.id_entry_list_item
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    if (viewType == R.id.id_entry_list_item) {
      return EntryViewHolder(inflater.inflate(R.layout.entry_list_item, parent, false), callback)
    } else {
      return SpaceViewHolder(inflater.inflate(R.layout.entry_space_item, parent, false))
    }
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    holder.bind(getItem(position))
  }

  override fun onViewRecycled(holder: ViewHolder) {
    super.onViewRecycled(holder)
    holder.unbind()
  }

  abstract class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view) {

    abstract fun bind(entry: FridgeEntry)

    abstract fun unbind()

  }

  class SpaceViewHolder internal constructor(view: View) : ViewHolder(view) {

    override fun bind(entry: FridgeEntry) {

    }

    override fun unbind() {

    }
  }

  class EntryViewHolder internal constructor(
    view: View,
    private val callback: Callback
  ) : ViewHolder(view) {

    private val entryName = itemView.findViewById<TextView>(R.id.entry_item_name)

    override fun bind(entry: FridgeEntry) {
      itemView.setOnDebouncedClickListener { callback.onItemClicked(entry) }
      entryName.text = entry.name()
    }

    override fun unbind() {
      itemView.setOnDebouncedClickListener(null)
      entryName.text = ""
    }

  }

  interface Callback {

    fun onItemClicked(entry: FridgeEntry)

  }

}
