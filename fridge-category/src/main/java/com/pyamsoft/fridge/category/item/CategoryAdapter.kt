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

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter

abstract class CategoryAdapter<VH : CategoryViewHolder> protected constructor() :
    ListAdapter<CategoryItemViewState, VH>(DIFFER) {

    final override fun onBindViewHolder(holder: VH, position: Int) {
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
    }
}
