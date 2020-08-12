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

package com.pyamsoft.fridge.category.item

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.bindViews
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnDestroy
import javax.inject.Inject

class CategoryViewHolder internal constructor(
    constraintLayout: ConstraintLayout,
    owner: LifecycleOwner,
    factory: CategoryItemComponent.Factory
) : RecyclerView.ViewHolder(constraintLayout) {

    private var viewBinder: ViewBinder<CategoryItemViewState>

    @Inject
    @JvmField
    internal var background: CategoryBackground? = null

    init {
        // Needs a small amount of margin so the staggered grid effect works
        factory.create(constraintLayout).inject(this)

        val backgroundView = requireNotNull(background)
        viewBinder = bindViews(
            owner,
            backgroundView
        ) {
            // TODO
        }

        constraintLayout.layout {
            backgroundView.also {
                connect(
                    it.id(),
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP
                )
                connect(
                    it.id(),
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START
                )
                connect(
                    it.id(),
                    ConstraintSet.END,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.END
                )
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
            }
        }

        owner.doOnDestroy {
            background = null
        }
    }

    fun bind(state: CategoryItemViewState) {
        viewBinder.bind(state)
    }
}
