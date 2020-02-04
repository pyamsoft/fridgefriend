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

package com.pyamsoft.fridge.detail.expand.categories

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.expand.ExpandCategoryComponentCreator
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import javax.inject.Inject

internal class ExpandedCategoryViewHolder internal constructor(
    itemView: View,
    owner: LifecycleOwner,
    callback: ExpandItemCategoryListAdapter.Callback,
    componentCreator: ExpandCategoryComponentCreator
) : RecyclerView.ViewHolder(itemView) {

    private val viewBinder: ViewBinder<ExpandedCategoryViewState>

    @JvmField
    @Inject
    internal var thumbnail: ExpandCategoryThumbnail? = null

    init {
        val parent = itemView.findViewById<ConstraintLayout>(R.id.expand_category_item)
        componentCreator.create(parent).inject(this)

        viewBinder = bindViews(
            owner,
            requireNotNull(thumbnail)
        ) {
            return@bindViews when (it) {
                is ExpandedCategoryViewEvent.Select -> callback.onCategorySelected(adapterPosition)
            }
        }
    }

    fun bind(state: ExpandedCategoryViewState) {
        viewBinder.bind(state)
    }
}
