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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.category.R
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

class SmallCategoryAdapter internal constructor(
    private val containerHeight: Int,
    private val owner: LifecycleOwner,
    private val factory: CategoryItemComponent.Factory
) : CategoryAdapter<SmallCategoryAdapter.BaseViewHolder>() {

    override fun getItemViewType(position: Int): Int {
        // Final item in the list is a placeholder
        return if (position >= itemCount - 1) VIEW_TYPE_PLACEHOLDER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_PLACEHOLDER) {
            val view = inflater.inflate(R.layout.category_placeholder_holder, parent, false)
            PlaceholderViewHolder(view, containerHeight)
        } else {
            val view = inflater.inflate(R.layout.category_small_holder, parent, false)
            SmallViewHolder(view, owner, factory)
        }
    }

    abstract class BaseViewHolder protected constructor(
        itemView: View
    ) : CategoryViewHolder(itemView)

    class SmallViewHolder internal constructor(
        itemView: View,
        owner: LifecycleOwner,
        factory: CategoryItemComponent.Factory
    ) : BaseViewHolder(itemView) {

        private var viewBinder: ViewBinder<CategoryItemViewState>

        @Inject
        @JvmField
        internal var background: CategoryBackground? = null

        init {
            val parent = itemView.findViewById<ConstraintLayout>(R.id.category_small_holder)
            factory.create(parent).inject(this)

            val background = requireNotNull(background)
            viewBinder = bindViews(
                owner,
                background
            ) {
                // TODO
            }

            parent.layout {
                background.also {
                    connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                    connect(
                        it.id(),
                        ConstraintSet.START,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.START
                    )
                    connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                    connect(
                        it.id(),
                        ConstraintSet.BOTTOM,
                        ConstraintSet.PARENT_ID,
                        ConstraintSet.BOTTOM
                    )
                }
            }
        }

        override fun bind(state: CategoryItemViewState) {
            viewBinder.bind(state)
        }
    }

    private class PlaceholderViewHolder internal constructor(
        itemView: View,
        containerHeight: Int
    ) : BaseViewHolder(itemView) {

        init {
            // Set the placeholder to be the size of the container
            // This allows us to finish the list view effect when scrolling the final item
            itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = containerHeight
            }
        }

        override fun bind(state: CategoryItemViewState) {
            // No-op
        }
    }

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_PLACEHOLDER = 1
    }
}
