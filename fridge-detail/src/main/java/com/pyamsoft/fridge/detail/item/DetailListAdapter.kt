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
 *
 */

package com.pyamsoft.fridge.detail.item

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import com.pyamsoft.pydroid.ui.databinding.ListitemFrameBinding

class DetailListAdapter constructor(
    private val owner: LifecycleOwner,
    private val editable: Boolean,
    private val defaultPresence: FridgeItem.Presence,
    private val callback: Callback,
    private val factory: DetailItemComponent.Factory
) : ListAdapter<DetailListItemViewState, DetailViewHolder<*>>(DIFFER) {

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.item.isEmpty()) {
            if (position == 0) R.id.id_item_top_space else R.id.id_item_bottom_space
        } else {
            R.id.id_item_list_item
        }
    }

    override fun getItemId(position: Int): Long {
        val item = getItem(position)
        return if (item.item.isEmpty()) {
            if (position == 0) 0 else itemCount - 1L
        } else {
            getItem(position).item.id()
                .hashCode()
                .toLong()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailViewHolder<*> {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.id.id_item_bottom_space, R.id.id_item_top_space -> {
                val binding = ListitemFrameBinding.inflate(inflater, parent, false)
                SpacerItemViewHolder(
                    binding,
                    if (viewType == R.id.id_item_top_space) TOP_SPACE else BOTTOM_SPACE
                )
            }
            else -> {
                val binding = DetailListItemHolderBinding.inflate(inflater, parent, false)
                val showGlances = defaultPresence == FridgeItem.Presence.HAVE
                if (showGlances) {
                    DetailItemGlancesViewHolder(binding, owner, editable, callback, factory)
                } else {
                    DetailItemDateViewHolder(binding, owner, editable, callback, factory)
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: DetailViewHolder<*>,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item)
    }

    interface Callback {

        fun onItemExpanded(index: Int)

        fun onPresenceChange(index: Int)
    }

    companion object {

        private const val TOP_SPACE = 12
        private const val BOTTOM_SPACE = 80

        private val DIFFER = object : DiffUtil.ItemCallback<DetailListItemViewState>() {

            override fun areItemsTheSame(
                oldItem: DetailListItemViewState,
                newItem: DetailListItemViewState
            ): Boolean {
                return oldItem.item.id() == newItem.item.id()
            }

            override fun areContentsTheSame(
                oldItem: DetailListItemViewState,
                newItem: DetailListItemViewState
            ): Boolean {
                return JsonMappableFridgeItem.from(oldItem.item) == JsonMappableFridgeItem.from(
                    newItem.item
                )
            }
        }
    }
}
