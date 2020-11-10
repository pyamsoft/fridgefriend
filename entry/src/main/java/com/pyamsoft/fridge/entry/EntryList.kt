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

package com.pyamsoft.fridge.entry

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.databinding.EntryListBinding
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.entry.item.EntryItemViewState
import com.pyamsoft.fridge.ui.applyToolbarOffset
import com.pyamsoft.fridge.ui.removeAllItemDecorations
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.util.asDp
import io.cabriole.decorator.DecorationLookup
import io.cabriole.decorator.LinearBoundsMarginDecoration
import io.cabriole.decorator.LinearMarginDecoration
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber
import javax.inject.Inject

class EntryList @Inject internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup,
    factory: EntryItemComponent.Factory
) : BaseUiView<EntryViewState, EntryViewEvent, EntryListBinding>(parent) {

    override val viewBinding = EntryListBinding::inflate

    override val layoutRoot by boundView { entryListRoot }

    private var modelAdapter: EntryListAdapter? = null
    private var bottomMarginDecoration: RecyclerView.ItemDecoration? = null
    private var lastScrollPosition = 0

    init {
        doOnInflate {
            layoutRoot.applyToolbarOffset()
        }

        doOnInflate {
            binding.entryList.layoutManager =
                GridLayoutManager(binding.entryList.context, 2).apply {
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = 3
                }
        }

        doOnInflate {
            modelAdapter = EntryListAdapter(
                owner = owner,
                factory = factory,
                callback = object : EntryListAdapter.Callback {

                    override fun onSelect(index: Int) {
                        publish(EntryViewEvent.SelectEntry(itemAtIndex(index)))
                    }
                })
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
            binding.entrySwipeRefresh.setOnRefreshListener { publish(EntryViewEvent.ForceRefresh) }
        }

        doOnInflate { savedInstanceState ->
            savedInstanceState.useIfAvailable<Int>(LAST_SCROLL_POSITION) { position ->
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

            outState.remove(LAST_SCROLL_POSITION)
        }

        doOnInflate {
            val margin = 16.asDp(binding.entryList.context)

            // Standard margin on all items
            // For some reason, the margin registers only half as large as it needs to
            // be, so we must double it.
            LinearMarginDecoration(bottomMargin = margin * 2).apply {
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
            removeBottomMargin()
            binding.entryList.removeAllItemDecorations()
        }

        doOnTeardown {
            // Throws - think this is because items have LifecycleObservers
            // binding.entryList.adapter = null
            clearList()

            binding.entrySwipeRefresh.setOnRefreshListener(null)

            modelAdapter = null
        }
    }

    @CheckResult
    private fun itemAtIndex(index: Int): FridgeEntry {
        return usingAdapter().currentList[index].entry
    }

    override fun onRender(state: EntryViewState) {
        handleBottomMargin(state)
        handleList(state)
        handleLoading(state)
    }

    @CheckResult
    private fun usingAdapter(): EntryListAdapter {
        return requireNotNull(modelAdapter)
    }

    private fun removeBottomMargin() {
        bottomMarginDecoration?.also { binding.entryList.removeItemDecoration(it) }
        bottomMarginDecoration = null
    }

    private fun setList(list: List<FridgeEntry>) {
        val data = list.map { EntryItemViewState(it) }
        usingAdapter().submitList(data)
    }

    private fun clearList() {
        usingAdapter().submitList(null)
    }

    private fun handleLoading(state: EntryViewState) {
        state.isLoading.let { loading ->
            binding.entrySwipeRefresh.refreshing(loading)
        }
    }

    private fun handleList(state: EntryViewState) {
        state.displayedEntries.let { entries ->
            when {
                entries.isEmpty() -> clearList()
                else -> setList(entries)
            }
        }
    }

    private fun handleBottomMargin(state: EntryViewState) {
        removeBottomMargin()
        state.bottomOffset.let { height ->
            if (height > 0) {
                // The bottom has additional space to fit the FAB
                val fabSpacing = 72.asDp(binding.entryList.context)
                LinearBoundsMarginDecoration(bottomMargin = fabSpacing + height).apply {
                    binding.entryList.addItemDecoration(this)
                    bottomMarginDecoration = this
                }
            }
        }
    }

    companion object {
        private const val LAST_SCROLL_POSITION = "entry_last_scroll_position"
    }
}
