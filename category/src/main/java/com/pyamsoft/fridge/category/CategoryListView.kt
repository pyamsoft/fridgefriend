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

package com.pyamsoft.fridge.category

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.pyamsoft.fridge.category.databinding.CategoryListViewBinding
import com.pyamsoft.fridge.category.item.CategoryAdapter
import com.pyamsoft.fridge.category.item.CategoryItemComponent
import com.pyamsoft.fridge.category.item.CategoryItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class CategoryListView @Inject internal constructor(
    factory: CategoryItemComponent.Factory,
    parent: ViewGroup
) : BaseUiView<CategoryViewState, CategoryViewEvent, CategoryListViewBinding>(parent) {

    override val viewBinding = CategoryListViewBinding::inflate

    override val layoutRoot by boundView { categoryList }

    private var modelAdapter: CategoryAdapter? = null
    private var animator: ViewPropertyAnimatorCompat? = null

    init {
        doOnInflate {
            binding.categoryList.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                    isItemPrefetchEnabled = true
                }

            modelAdapter = CategoryAdapter(factory)
            binding.categoryList.adapter = modelAdapter
        }

        doOnTeardown {
            binding.categoryList.adapter = null
            clearList()

            modelAdapter = null
        }

        doOnTeardown {
            animator?.cancel()
            animator = null
        }
    }

    private fun clearList() {
        modelAdapter?.submitList(null)
    }

    @CheckResult
    private fun usingAdapter(): CategoryAdapter {
        return requireNotNull(modelAdapter)
    }

    override fun onRender(state: CategoryViewState) {
        handleCategories(state)
    }

    private fun handleAnimation(state: CategoryViewState) {
        state.categories.let { categories ->
            if (categories.isNotEmpty()) {
                // If root is currently hidden, show it
                if (animator == null) {
                    animator =
                        com.pyamsoft.fridge.ui.animatePopInFromBottom(layoutRoot)
                }
            }
        }
    }

    private fun handleCategories(state: CategoryViewState) {
        state.categories.let { categories ->
            if (categories.isEmpty()) {
                clearList()
            } else {
                usingAdapter().submitList(categories.map { pairing ->
                    CategoryItemViewState(
                        pairing.category,
                        pairing.items.asSequence().map { it.count() }.sum()
                    )
                })
            }
        }
    }
}
