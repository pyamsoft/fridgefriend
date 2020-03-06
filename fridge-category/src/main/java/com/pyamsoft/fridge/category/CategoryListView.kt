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

package com.pyamsoft.fridge.category

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.pyamsoft.fridge.category.databinding.CategoryListViewBinding
import com.pyamsoft.fridge.category.item.CategoryAdapter
import com.pyamsoft.fridge.category.item.CategoryItemComponent
import com.pyamsoft.fridge.category.item.CategoryItemViewState
import com.pyamsoft.pydroid.arch.BindingUiView
import javax.inject.Inject

class CategoryListView @Inject internal constructor(
    factory: CategoryItemComponent.Factory,
    owner: LifecycleOwner,
    parent: ViewGroup
) : BindingUiView<CategoryViewState, CategoryViewEvent, CategoryListViewBinding>(parent) {

    override val viewBinding = CategoryListViewBinding::inflate

    override val layoutRoot by boundView { categoryList }

    private var modelAdapter: CategoryAdapter? = null

    init {
        doOnInflate {
            binding.categoryList.layoutManager =
                StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL).apply {
                    isItemPrefetchEnabled = true
                }

            modelAdapter = CategoryAdapter(owner, factory)
            binding.categoryList.adapter = usingAdapter().apply { setHasStableIds(true) }
            binding.categoryList.setHasFixedSize(true)
        }

        doOnTeardown {
            clearList()

            modelAdapter = null
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
