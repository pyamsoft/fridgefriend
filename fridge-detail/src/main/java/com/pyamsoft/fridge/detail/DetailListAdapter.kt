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

package com.pyamsoft.fridge.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemComponent

internal class DetailListAdapter constructor(
    private val editable: Boolean,
    private val injectComponent: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent,
    private val callback: Callback
) : ListAdapter<FridgeItem, DetailViewHolder>(object : DiffUtil.ItemCallback<FridgeItem>() {

    override fun areItemsTheSame(
        oldItem: FridgeItem,
        newItem: FridgeItem
    ): Boolean {
        return oldItem.id() == newItem.id()
    }

    override fun areContentsTheSame(
        oldItem: FridgeItem,
        newItem: FridgeItem
    ): Boolean {
        return JsonMappableFridgeItem.from(oldItem) == JsonMappableFridgeItem.from(newItem)
    }
}) {

    @CheckResult
    private fun isEmptyItem(position: Int): Boolean {
        return position == 0
    }

    override fun getItemViewType(position: Int): Int {
        if (isEmptyItem(position)) {
            return R.id.id_item_empty_item
        } else {
            return R.id.id_item_list_item
        }
    }

    override fun getItemId(position: Int): Long {
        if (isEmptyItem(position)) {
            return 0
        } else {
            return getItem(position).id()
                .hashCode()
                .toLong()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        if (viewType == R.id.id_item_empty_item) {
            val v = inflater.inflate(R.layout.listitem_frame, parent, false)
            return SpacerItemViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.listitem_constraint, parent, false)
            return DetailItemViewHolder(v, injectComponent)
        }
    }

    override fun onBindViewHolder(
        holder: DetailViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item, editable, callback)
    }

    override fun onViewRecycled(holder: DetailViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }

    internal abstract class DetailViewHolder protected constructor(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        abstract fun bind(
            item: FridgeItem,
            editable: Boolean,
            callback: Callback
        )

        abstract fun unbind()
    }

    interface Callback {

        fun onItemExpanded(item: FridgeItem)

        fun onPickDate(
            oldItem: FridgeItem,
            year: Int,
            month: Int,
            day: Int
        )
    }
}
