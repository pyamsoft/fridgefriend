/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail.expand.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.pyamsoft.fridge.detail.databinding.ExpandCategoryItemHolderBinding
import com.pyamsoft.pydroid.ui.theme.ThemeProvider

internal class ExpandItemCategoryListAdapter
internal constructor(
    private val owner: LifecycleOwner,
    private val themeProvider: ThemeProvider,
    private val factory: ExpandCategoryComponent.Factory,
    private val callback: Callback
) : ListAdapter<ExpandedCategoryViewState, ExpandedCategoryViewHolder>(DIFFER) {

  init {
    setHasStableIds(true)
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).category?.id?.hashCode()?.toLong() ?: 0L
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpandedCategoryViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val binding = ExpandCategoryItemHolderBinding.inflate(inflater, parent, false)
    return ExpandedCategoryViewHolder(binding, owner, themeProvider, factory, callback)
  }

  override fun onBindViewHolder(holder: ExpandedCategoryViewHolder, position: Int) {
    val category = getItem(position)
    holder.bindState(category)
  }

  interface Callback {

    fun onCategorySelected(index: Int)
  }

  companion object {

    private val DIFFER =
        object : DiffUtil.ItemCallback<ExpandedCategoryViewState>() {

          override fun areItemsTheSame(
              oldItem: ExpandedCategoryViewState,
              newItem: ExpandedCategoryViewState
          ): Boolean {
            return oldItem.category?.id == newItem.category?.id
          }

          override fun areContentsTheSame(
              oldItem: ExpandedCategoryViewState,
              newItem: ExpandedCategoryViewState
          ): Boolean {
            return oldItem.category == newItem.category && oldItem.isSelected == newItem.isSelected
          }
        }
  }
}
