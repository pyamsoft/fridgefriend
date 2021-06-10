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

package com.pyamsoft.fridge.category

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.pyamsoft.fridge.category.databinding.CategoryListViewBinding
import com.pyamsoft.fridge.category.item.CategoryAdapter
import com.pyamsoft.fridge.category.item.CategoryItemComponent
import com.pyamsoft.fridge.category.item.CategoryItemViewState
import com.pyamsoft.fridge.ui.animatePopInFromBottom
import com.pyamsoft.fridge.ui.doOnChildRemoved
import com.pyamsoft.fridge.ui.teardownViewHolderAt
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UnitViewEvent
import javax.inject.Inject

class CategoryListView
@Inject
internal constructor(
    factory: CategoryItemComponent.Factory,
    parent: ViewGroup,
) : BaseUiView<CategoryViewState, UnitViewEvent, CategoryListViewBinding>(parent) {

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

      modelAdapter =
          CategoryAdapter(factory).apply {
            val registration = doOnChildRemoved { binding.categoryList.teardownViewHolderAt(it) }
            doOnTeardown { registration.unregister() }
          }
      binding.categoryList.adapter = modelAdapter
    }

    doOnTeardown {
      binding.categoryList.adapter = null

      modelAdapter = null
    }

    doOnTeardown {
      animator?.cancel()
      animator = null
    }
  }

  private fun clearList() {
    usingAdapter().submitList(null)
  }

  @CheckResult
  private fun usingAdapter(): CategoryAdapter {
    return requireNotNull(modelAdapter)
  }

  override fun onRender(state: UiRender<CategoryViewState>) {
    state.mapChanged { it.categories }.render(viewScope) { handleAnimation(it) }
    state.mapChanged { it.categories }.render(viewScope) { handleCategories(it) }
  }

  private fun handleAnimation(categories: List<CategoryViewState.CategoryItemsPairing>) {
    if (categories.isNotEmpty()) {
      // If root is currently hidden, show it
      if (animator == null) {
        animator = animatePopInFromBottom(layoutRoot)
      }
    }
  }

  private fun handleCategories(categories: List<CategoryViewState.CategoryItemsPairing>) {
    if (categories.isEmpty()) {
      clearList()
    } else {
      usingAdapter()
          .submitList(
              categories.map { p ->
                CategoryItemViewState(p.category, p.items.asSequence().map { it.count() }.sum())
              })
    }
  }
}
