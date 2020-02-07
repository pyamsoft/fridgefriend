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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.category.item.CategoryItemComponent
import com.pyamsoft.fridge.category.item.CategoryItemViewState
import com.pyamsoft.fridge.category.item.LargeCategoryAdapter
import com.pyamsoft.fridge.category.item.SmallCategoryAdapter
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class CategoryListView @Inject internal constructor(
    private val factory: CategoryItemComponent.Factory,
    parent: ViewGroup
) : BaseUiView<CategoryViewState, CategoryViewEvent>(parent) {

    override val layout: Int = R.layout.category_list_view

    override val layoutRoot by boundView<ViewGroup>(R.id.category_list_root)
    private val largeList by boundView<RecyclerView>(R.id.category_list_large)
    private val smallList by boundView<RecyclerView>(R.id.category_list_small)

    private val scrollRatio by lazy(LazyThreadSafetyMode.NONE) {
        val resources = parent.context.applicationContext.resources
        val largeSize = resources.getDimension(R.dimen.list_item_large)
        val smallSize = resources.getDimension(R.dimen.list_item_small)
        largeSize / smallSize
    }

    private var largeAdapter: LargeCategoryAdapter? = null
    private var smallAdapter: SmallCategoryAdapter? = null

    private var isLargeListScrollRegistered = false
    private var isSmallListScrollRegistered = false

    private val largeListScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            // Once the scroll on the large state is done, re-register the small listener
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                registerSmallListScrollListener()
            } else {
                // Otherwise, unregister the small listener
                unregisterSmallListScrollListener()
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            // Scale scrolls on the large list down to the small ratio
            val smallDy = (dy.toFloat() / scrollRatio).toInt()
            smallList.scrollBy(0, smallDy)
        }
    }

    private val smallListScrollListener = object : RecyclerView.OnScrollListener() {

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            // Once the scroll on the small state is done, re-register the large listener
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                registerLargeListScrollListener()
            } else {
                // Otherwise, unregister the large listener
                unregisterLargeListScrollListener()
            }
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            // Scale scrolls on the large list down to the small ratio
            val smallDy = (dy.toFloat() * scrollRatio).toInt()
            largeList.scrollBy(0, smallDy)
        }
    }

    init {
        doOnInflate {
            largeList.layoutManager = LinearLayoutManager(largeList.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
            }

            val a = LargeCategoryAdapter(factory)
            largeAdapter = a
            largeList.adapter = a

            registerLargeListScrollListener()
        }

        doOnInflate {
            smallList.layoutManager = LinearLayoutManager(smallList.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
            }

            // Wait for post() to finish so that we are guaranteed the smallList view is measured.
            smallList.post {
                val a = SmallCategoryAdapter(smallList.height, factory)
                smallAdapter = a
                smallList.adapter = a

                registerSmallListScrollListener()
            }
        }

        doOnTeardown {
            unregisterLargeListScrollListener()
            unregisterSmallListScrollListener()
        }

        doOnTeardown {
            clearLists()

            largeAdapter = null
            smallAdapter = null
        }
    }

    private fun clearLists() {
        clearLargeList()
        clearSmallList()
    }

    private fun clearLargeList() {
        largeAdapter?.submitList(null)
    }

    private fun clearSmallList() {
        largeAdapter?.submitList(null)
    }

    @CheckResult
    private fun usingLargeAdapter(): LargeCategoryAdapter {
        return requireNotNull(largeAdapter)
    }

    @CheckResult
    private fun usingSmallAdapter(): SmallCategoryAdapter {
        return requireNotNull(smallAdapter)
    }

    private fun registerLargeListScrollListener() {
        if (!isLargeListScrollRegistered) {
            isLargeListScrollRegistered = true
            largeList.addOnScrollListener(largeListScrollListener)
        }
    }

    private fun unregisterLargeListScrollListener() {
        if (isLargeListScrollRegistered) {
            isLargeListScrollRegistered = false
            largeList.removeOnScrollListener(largeListScrollListener)
        }
    }

    private fun registerSmallListScrollListener() {
        if (!isSmallListScrollRegistered) {
            isSmallListScrollRegistered = true
            smallList.addOnScrollListener(smallListScrollListener)
        }
    }

    private fun unregisterSmallListScrollListener() {
        if (isSmallListScrollRegistered) {
            isSmallListScrollRegistered = false
            smallList.removeOnScrollListener(smallListScrollListener)
        }
    }

    override fun onRender(state: CategoryViewState) {
        state.largeCategories.let { categories ->
            if (categories.isEmpty()) {
                clearLargeList()
            } else {
                usingLargeAdapter().submitList(categories.map { CategoryItemViewState(it) })
            }
        }

        state.smallCategories.let { categories ->
            if (categories.isEmpty()) {
                clearSmallList()
            } else {
                usingSmallAdapter().submitList(categories.map { CategoryItemViewState(it) })
            }
        }
    }
}
