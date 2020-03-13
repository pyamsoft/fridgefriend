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

import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.detail.databinding.ExpandCategoryItemHolderBinding
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

class ExpandedCategoryViewHolder internal constructor(
    binding: ExpandCategoryItemHolderBinding,
    owner: LifecycleOwner,
    callback: ExpandItemCategoryListAdapter.Callback,
    factory: ExpandCategoryComponent.Factory
) : RecyclerView.ViewHolder(binding.root) {

    private val viewBinder: ViewBinder<ExpandedCategoryViewState>

    @JvmField
    @Inject
    internal var thumbnail: ExpandCategoryThumbnail? = null

    @JvmField
    @Inject
    internal var name: ExpandCategoryName? = null

    @JvmField
    @Inject
    internal var scrim: ExpandCategoryScrim? = null

    @JvmField
    @Inject
    internal var selectOverlay: ExpandCategorySelectOverlay? = null

    init {
        factory.create(binding.expandCategoryItem).inject(this)

        val thumbnail = requireNotNull(thumbnail)
        val scrim = requireNotNull(scrim)
        val name = requireNotNull(name)
        val selectOverlay = requireNotNull(selectOverlay)
        viewBinder = bindViews(
            owner,
            thumbnail,
            scrim,
            selectOverlay,
            name
        ) {
            return@bindViews when (it) {
                is ExpandedCategoryViewEvent.Select -> callback.onCategorySelected(adapterPosition)
            }
        }

        binding.expandCategoryItem.layout {
            thumbnail.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }

            scrim.also {
                connect(
                    it.id(), ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }

            name.also {
                connect(
                    it.id(), ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(
                    it.id(), ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(
                    it.id(), ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
            }
        }
    }

    fun bind(state: ExpandedCategoryViewState) {
        viewBinder.bind(state)
    }
}
