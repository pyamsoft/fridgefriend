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

package com.pyamsoft.fridge.entry

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pyamsoft.fridge.entry.databinding.EntryListBinding
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.entry.item.EntryItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.ui.util.removeAllItemDecorations
import com.pyamsoft.pydroid.util.asDp
import io.cabriole.decorator.DecorationLookup
import io.cabriole.decorator.LinearBoundsMarginDecoration
import io.cabriole.decorator.LinearMarginDecoration
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber

abstract class BaseEntryList<V : UiViewEvent> protected constructor(
    parent: ViewGroup,
    factory: EntryItemComponent.Factory,
) : BaseUiView<EntryViewState, V, EntryListBinding>(parent),
    SwipeRefreshLayout.OnRefreshListener,
    EntryListAdapter.Callback {

    final override val viewBinding = EntryListBinding::inflate

    final override val layoutRoot by boundView { entryListRoot }

    private var modelAdapter: EntryListAdapter? = null

    private var lastScrollPosition = 0

    init {
        doOnInflate {
            binding.entryList.layoutManager =
                GridLayoutManager(binding.entryList.context, 2).apply {
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = 3
                }
        }

        doOnInflate {
            modelAdapter = EntryListAdapter(factory = factory, callback = this)
            binding.entryList.adapter = modelAdapter
        }

        doOnInflate {
            // Fast Scroll
            FastScrollerBuilder(binding.entryList)
                .useMd2Style()
                .setPopupTextProvider(usingAdapter())
                .build()
        }

        doOnInflate {
            binding.entrySwipeRefresh.setOnRefreshListener(this)
        }

        doOnInflate { savedInstanceState ->
            val position = savedInstanceState.get(LAST_SCROLL_POSITION) ?: -1
            if (position >= 0) {
                Timber.d("Last scroll position saved at: $position")
                lastScrollPosition = position
            }
        }

        doOnSaveState { outState ->
            val manager = binding.entryList.layoutManager
            if (manager is GridLayoutManager) {
                val position = manager.findFirstVisibleItemPosition()
                if (position > 0) {
                    outState.put(LAST_SCROLL_POSITION, position)
                    return@doOnSaveState
                }
            }

            outState.remove<Nothing>(LAST_SCROLL_POSITION)
        }

        doOnInflate {
            val margin = 16.asDp(binding.entryList.context)

            // Standard margin on all items
            // For some reason, the margin registers only half as large as it needs to
            // be, so we must double it.
            LinearMarginDecoration(bottomMargin = margin * 2).apply {
                binding.entryList.addItemDecoration(this)
            }

            // The bottom has additional space to fit the FAB
            val bottomMargin = 72.asDp(binding.entryList.context)
            LinearBoundsMarginDecoration(bottomMargin = bottomMargin).apply {
                binding.entryList.addItemDecoration(this)
            }

            // Left margin on items on the left
            LinearMarginDecoration(
                leftMargin = margin,
                // Half margin since these cards will meet in the middle
                rightMargin = margin / 2,
                decorationLookup = object : DecorationLookup {
                    override fun shouldApplyDecoration(position: Int, itemCount: Int): Boolean {
                        // If the position is even, its on the left side and should have a left margin
                        // Bitwise is faster than modulo
                        return position and 1 == 0
                    }
                })
                .apply {
                    binding.entryList.addItemDecoration(this)
                }

            // Right margin on items on the right
            LinearMarginDecoration(
                rightMargin = margin,
                // Half margin since these cards will meet in the middle
                leftMargin = margin / 2,
                decorationLookup = object : DecorationLookup {
                    override fun shouldApplyDecoration(position: Int, itemCount: Int): Boolean {
                        // If the position is odd, its on the right side and should have a right margin
                        // Bitwise is faster than modulo
                        return position and 1 == 1
                    }
                })
                .apply {
                    binding.entryList.addItemDecoration(this)
                }
        }

        doOnTeardown {
            binding.entryList.removeAllItemDecorations()
        }

        doOnTeardown {
            binding.entryList.adapter = null

            binding.entrySwipeRefresh.setOnRefreshListener(null)

            modelAdapter = null
        }
    }

    final override fun onRender(state: UiRender<EntryViewState>) {
        state.mapChanged { it.displayedEntries }.render(viewScope) { handleList(it) }
        state.mapChanged { it.isLoading }.render(viewScope) { handleLoading(it) }
    }

    @CheckResult
    private fun usingAdapter(): EntryListAdapter {
        return requireNotNull(modelAdapter)
    }

    private fun setList(list: List<EntryViewState.EntryGroup>) {
        val data = list.map { EntryItemViewState(it.entry, itemCount = it.items.size) }
        Timber.d("Submit data list: $data")
        usingAdapter().submitList(data)
    }

    private fun clearList() {
        usingAdapter().submitList(null)
    }

    private fun handleLoading(loading: Boolean) {
        binding.entrySwipeRefresh.isRefreshing = loading
    }

    private fun handleList(entries: List<EntryViewState.EntryGroup>) {
        when {
            entries.isEmpty() -> clearList()
            else -> setList(entries)
        }
    }

    companion object {
        private const val LAST_SCROLL_POSITION = "entry_last_scroll_position"
    }
}
