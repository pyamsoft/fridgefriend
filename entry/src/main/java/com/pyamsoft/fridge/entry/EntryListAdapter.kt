/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.entry.databinding.EntryListItemHolderBinding
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.entry.item.EntryItemViewHolder
import com.pyamsoft.fridge.entry.item.EntryItemViewState
import me.zhanghai.android.fastscroll.PopupTextProvider

class EntryListAdapter internal constructor(
    private val owner: LifecycleOwner,
    private val factory: EntryItemComponent.Factory,
    private val callback: Callback
) : ListAdapter<EntryItemViewState, EntryItemViewHolder>(DIFFER), PopupTextProvider {

    init {
        setHasStableIds(true)
    }

    override fun getPopupText(position: Int): String {
        val item = getItem(position)
        return item.entry.name()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = EntryListItemHolderBinding.inflate(inflater, parent, false)
        return EntryItemViewHolder(binding, owner, factory, callback)
    }

    override fun onBindViewHolder(holder: EntryItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    interface Callback {

        fun onSelect(index: Int)

    }

    companion object {

        private val DIFFER = object : DiffUtil.ItemCallback<EntryItemViewState>() {

            override fun areItemsTheSame(
                oldItem: EntryItemViewState,
                newItem: EntryItemViewState
            ): Boolean {
                return oldItem.entry.id() == newItem.entry.id()
            }

            override fun areContentsTheSame(
                oldItem: EntryItemViewState,
                newItem: EntryItemViewState
            ): Boolean {
                return JsonMappableFridgeEntry.from(oldItem.entry) == JsonMappableFridgeEntry.from(
                    newItem.entry
                )
            }
        }
    }

}
