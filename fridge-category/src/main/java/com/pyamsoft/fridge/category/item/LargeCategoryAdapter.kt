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
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.pyamsoft.fridge.category.R

class LargeCategoryAdapter internal constructor(
    private val factory: CategoryItemComponent.Factory
) : CategoryAdapter<LargeCategoryAdapter.LargeViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LargeViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.category_large_holder, parent, false)
        return LargeViewHolder(view, factory)
    }

    class LargeViewHolder internal constructor(
        itemView: View,
        factory: CategoryItemComponent.Factory
    ) : CategoryViewHolder(itemView) {

        init {
            val parent = itemView.findViewById<ConstraintLayout>(R.id.category_large_holder)
            factory.create(parent).inject(this)
        }

        override fun bind(state: CategoryItemViewState) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
