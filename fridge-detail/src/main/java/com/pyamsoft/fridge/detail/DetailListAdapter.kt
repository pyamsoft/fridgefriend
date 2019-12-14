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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailListItemViewState
import com.pyamsoft.pydroid.arch.ViewBinder

internal class DetailListAdapter constructor(
    private val owner: LifecycleOwner,
    private val editable: Boolean,
    private val callback: Callback,
    private val componentCreator: DetailListItemComponentCreator
) : ListAdapter<DetailListItemViewState, DetailViewHolder>(DIFFER) {

    override fun getItemViewType(position: Int): Int {
        return if (isEmptyItem(position)) {
            R.id.id_item_empty_item
        } else {
            R.id.id_item_list_item
        }
    }

    override fun getItemId(position: Int): Long {
        return if (isEmptyItem(position)) 0 else {
            getItem(position).item.id()
                .hashCode()
                .toLong()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DetailViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == R.id.id_item_empty_item) {
            val v = inflater.inflate(R.layout.listitem_frame, parent, false)
            SpacerItemViewHolder(v)
        } else {
            val v = inflater.inflate(R.layout.detail_list_item_holder, parent, false)
            DetailItemViewHolder(v, owner, editable, callback, componentCreator)
        }
    }

    override fun onBindViewHolder(
        holder: DetailViewHolder,
        position: Int
    ) {
        val item = getItem(position)
        holder.bind(item)
    }

    internal abstract class DetailViewHolder protected constructor(
        view: View
    ) : RecyclerView.ViewHolder(view), ViewBinder<DetailListItemViewState>

    interface Callback {

        fun onItemExpanded(index: Int)

        fun onPresenceChange(index: Int)
    }

    companion object {

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

        @JvmStatic
        @CheckResult
        private fun isEmptyItem(position: Int): Boolean {
            return position == 0
        }
    }
}
