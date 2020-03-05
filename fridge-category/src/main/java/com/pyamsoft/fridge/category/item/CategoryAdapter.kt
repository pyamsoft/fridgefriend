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

package com.pyamsoft.fridge.category.item

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pyamsoft.fridge.category.databinding.CategoryItemHolderLargeBinding
import com.pyamsoft.fridge.category.databinding.CategoryItemHolderMediumBinding
import com.pyamsoft.fridge.category.databinding.CategoryItemHolderSmallBinding

class CategoryAdapter internal constructor(
    private val owner: LifecycleOwner,
    private val factory: CategoryItemComponent.Factory
) : ListAdapter<CategoryItemViewState, CategoryViewHolder>(DIFFER) {

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return when {
            item.itemCount <= 0 -> VIEW_TYPE_SMALL
            item.itemCount <= 5 -> VIEW_TYPE_MEDIUM
            else -> VIEW_TYPE_LARGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = when (viewType) {
            VIEW_TYPE_SMALL -> CategoryItemHolderSmallBinding
                .inflate(inflater, parent, false).categoryHolder
            VIEW_TYPE_MEDIUM -> CategoryItemHolderMediumBinding
                .inflate(inflater, parent, false).categoryHolder
            else -> CategoryItemHolderLargeBinding
                .inflate(inflater, parent, false).categoryHolder
        }
        return CategoryViewHolder(
            view,
            owner,
            factory
        )
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val state = getItem(position)
        holder.bind(state)
    }

    companion object {

        private val DIFFER = object : DiffUtil.ItemCallback<CategoryItemViewState>() {
            override fun areItemsTheSame(
                oldItem: CategoryItemViewState,
                newItem: CategoryItemViewState
            ): Boolean {
                return oldItem.category.id() == newItem.category.id()
            }

            override fun areContentsTheSame(
                oldItem: CategoryItemViewState,
                newItem: CategoryItemViewState
            ): Boolean {
                return oldItem == newItem
            }
        }

        private const val VIEW_TYPE_LARGE = 0
        private const val VIEW_TYPE_MEDIUM = 1
        private const val VIEW_TYPE_SMALL = 2
    }
}
