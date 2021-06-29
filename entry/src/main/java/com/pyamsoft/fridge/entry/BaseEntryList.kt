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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import io.cabriole.decorator.LinearMarginDecoration
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber

abstract class BaseEntryList<V : UiViewEvent>
protected constructor(
    owner: LifecycleOwner,
    parent: ViewGroup,
    factory: EntryItemComponent.Factory,
) :
    BaseUiView<EntryViewState, V, EntryListBinding>(parent),
    SwipeRefreshLayout.OnRefreshListener,
    EntryListAdapter.Callback {

  final override val viewBinding = EntryListBinding::inflate

  final override val layoutRoot by boundView { entrySwipeRefresh }

  private var modelAdapter: EntryListAdapter? = null

  private var lastScrollPosition = 0

  init {
    doOnInflate {
      binding.entryList.layoutManager =
          LinearLayoutManager(binding.entryList.context).apply {
        isItemPrefetchEnabled = true
        initialPrefetchItemCount = 3
      }
    }

    doOnInflate {
      modelAdapter = EntryListAdapter(owner = owner, factory = factory, callback = this)
      binding.entryList.adapter = modelAdapter
    }

    doOnInflate {
      // Fast Scroll
      FastScrollerBuilder(binding.entryList)
          .useMd2Style()
          .setPopupTextProvider(usingAdapter())
          .build()
    }

    doOnInflate { binding.entrySwipeRefresh.setOnRefreshListener(this) }

    doOnInflate { savedInstanceState ->
      val position = savedInstanceState.get(LAST_SCROLL_POSITION) ?: -1
      if (position >= 0) {
        Timber.d("Last scroll position saved at: $position")
        lastScrollPosition = position
      }
    }

    doOnSaveState { outState ->
      val manager = binding.entryList.layoutManager
      if (manager is LinearLayoutManager) {
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

      LinearMarginDecoration.createHorizontal(
              horizontalMargin = margin, orientation = RecyclerView.VERTICAL)
          .apply { binding.entryList.addItemDecoration(this) }

      // Everything has a bottom margin
      LinearMarginDecoration(bottomMargin = margin, orientation = RecyclerView.VERTICAL).apply {
        binding.entryList.addItemDecoration(this)
      }

      // Everything but first item has a top margin
      LinearMarginDecoration(
              topMargin = margin,
              orientation = RecyclerView.VERTICAL,
              decorationLookup =
                  object : DecorationLookup {
                    override fun shouldApplyDecoration(position: Int, itemCount: Int): Boolean {
                      return position > 0
                    }
                  })
          .apply { binding.entryList.addItemDecoration(this) }
    }

    doOnTeardown { binding.entryList.removeAllItemDecorations() }

    doOnTeardown {
      binding.entryList.adapter = null

      binding.entrySwipeRefresh.setOnRefreshListener(null)

      modelAdapter = null
    }
  }

  final override fun onRender(state: UiRender<EntryViewState>) {
    state.mapChanged { it.displayedEntries }.render(viewScope) { handleList(it) }
    state.mapChanged { it.isLoading }.render(viewScope) { handleLoading(it) }
    state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomOffset(it) }
  }

  protected open fun handleBottomOffset(height: Int) {}

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
