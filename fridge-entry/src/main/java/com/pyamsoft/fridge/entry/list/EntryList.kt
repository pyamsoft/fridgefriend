/*
 * Copyright 2019 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.entry.list

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.commons.utils.FastAdapterDiffUtil
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject

internal class EntryList @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<EntryList.Callback>(parent, callback) {

  override val layout: Int = R.layout.entry_list

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.entry_swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.entry_list)
  private val emptyState by lazyView<TextView>(R.id.entry_empty)

  private var modelAdapter: ModelAdapter<FridgeEntry, EntryItem<*, *>>? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    modelAdapter = ModelAdapter { entry ->
      if (entry.id().isNotBlank()) {
        return@ModelAdapter EntryListItem(entry, callback)
      } else {
        return@ModelAdapter EntrySpaceItem(entry)
      }
    }

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    recyclerView.adapter =
      FastAdapter.with<EntryItem<*, *>, ModelAdapter<FridgeEntry, *>>(usingAdapter()).apply {
        setHasStableIds(true)
      }

    layoutRoot.setOnRefreshListener {
      callback.onRefresh()
    }
  }

  override fun onTeardown() {
    super.onTeardown()
    usingAdapter().clear()
    recyclerView.adapter = null
    modelAdapter = null

    layoutRoot.setOnRefreshListener(null)
  }

  @CheckResult
  private fun usingAdapter(): ModelAdapter<FridgeEntry, EntryItem<*, *>> {
    return requireNotNull(modelAdapter)
  }

  private fun showList() {
    emptyState.isVisible = false
    recyclerView.isVisible = true
  }

  private fun hideList() {
    recyclerView.isVisible = false
    emptyState.isVisible = true
  }

  fun beginRefresh() {
    layoutRoot.refreshing(true)
    showList()
  }

  fun setList(entries: List<FridgeEntry>) {
    val list = arrayListOf(FridgeEntry.empty()) + entries
    val items = list.map { usingAdapter().intercept(it) }
    FastAdapterDiffUtil.set(usingAdapter(), items, true)
  }

  fun clearList() {
    usingAdapter().clear()
  }

  fun showError(throwable: Throwable) {
    // TODO set error text
  }

  fun clearError() {
    // TODO clear error
  }

  fun finishRefresh() {
    layoutRoot.refreshing(false)

    if (usingAdapter().adapterItemCount <= 1) {
      hideList()
    } else {
      showList()
    }
  }

  interface Callback : EntryListItem.Callback {

    fun onRefresh()

  }
}
