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

package com.pyamsoft.fridge.detail.item

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import com.pyamsoft.pydroid.ui.util.teardownAdapter
import me.zhanghai.android.fastscroll.PopupTextProvider

class DetailListAdapter internal constructor(
    private val factory: DetailItemComponent.Factory,
    private val callback: Callback
) : ListAdapter<DetailItemViewState, DetailItemViewHolder>(DIFFER), PopupTextProvider {

    init {
        setHasStableIds(true)
    }

    override fun getPopupText(position: Int): String {
        val item = getItem(position)
        return item.item.name()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).item.id().id.hashCode().toLong()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = DetailListItemHolderBinding.inflate(inflater, parent, false)
        return DetailItemViewHolder(binding, editable = false, factory, callback)
    }

    override fun onBindViewHolder(
        holder: DetailItemViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        teardownAdapter(recyclerView)
    }

    interface Callback {

        fun onIncreaseCount(index: Int)

        fun onDecreaseCount(index: Int)

        fun onItemExpanded(index: Int)

        fun onPresenceChange(index: Int)
    }

    companion object {

        private val DIFFER = object : DiffUtil.ItemCallback<DetailItemViewState>() {

            override fun areItemsTheSame(
                oldItem: DetailItemViewState,
                newItem: DetailItemViewState
            ): Boolean {
                return oldItem.item.id() == newItem.item.id()
            }

            override fun areContentsTheSame(
                oldItem: DetailItemViewState,
                newItem: DetailItemViewState
            ): Boolean {
                return JsonMappableFridgeItem.from(oldItem.item) == JsonMappableFridgeItem.from(
                    newItem.item
                ) && oldItem.expirationRange == newItem.expirationRange && oldItem.isSameDayExpired == newItem.isSameDayExpired
            }
        }
    }
}
