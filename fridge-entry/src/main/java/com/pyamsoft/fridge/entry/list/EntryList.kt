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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.popinnow.android.refresh.RefreshLatch
import com.popinnow.android.refresh.newRefreshLatch
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.fridge.entry.list.EntryListViewEvent.ForceRefresh
import com.pyamsoft.fridge.entry.list.EntryListViewEvent.OpenEntry
import com.pyamsoft.pydroid.arch.impl.BaseUiView
import com.pyamsoft.pydroid.arch.impl.onChange
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject

class EntryList @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<EntryListViewState, EntryListViewEvent>(parent) {

  override val layout: Int = R.layout.entry_list

  override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.entry_swipe_refresh)

  private val recyclerView by boundView<RecyclerView>(R.id.entry_list)
  private val emptyState by boundView<TextView>(R.id.entry_empty)

  private var modelAdapter: EntryListAdapter? = null
  private var refreshLatch: RefreshLatch? = null

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    modelAdapter = EntryListAdapter(object : EntryListAdapter.Callback {
      override fun onItemClicked(entry: FridgeEntry) {
        publish(OpenEntry(entry))
      }
    })

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    recyclerView.adapter = modelAdapter

    layoutRoot.setOnRefreshListener {
      publish(ForceRefresh)
    }

    refreshLatch = newRefreshLatch(owner) { isRefreshing ->
      layoutRoot.refreshing(isRefreshing)
      if (isRefreshing) {
        showList()
      } else {
        if (usingAdapter().itemCount == 0) {
          hideList()
        } else {
          showList()
        }
      }
    }
  }

  override fun onTeardown() {
    super.onTeardown()
    clearList()
    clearError()

    recyclerView.adapter = null
    layoutRoot.setOnRefreshListener(null)

    modelAdapter = null
    refreshLatch = null
  }

  @CheckResult
  private fun usingAdapter(): EntryListAdapter {
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

  private fun setList(entries: List<FridgeEntry>) {
    usingAdapter().submitList(listOf(FridgeEntry.empty()) + entries)
  }

  private fun clearList() {
    usingAdapter().submitList(null)
  }

  private fun showError(throwable: Throwable) {
    Snackbreak.bindTo(owner)
        .short(layoutRoot, throwable.message ?: "Error refreshing list, please try again")
        .show()
  }

  private fun clearError() {
    Snackbreak.bindTo(owner)
        .dismiss()
  }

  override fun onRender(
    state: EntryListViewState,
    oldState: EntryListViewState?
  ) {
    state.onChange(oldState, field = { it.isLoading }) { loading ->
      if (loading != null) {
        requireNotNull(refreshLatch).isRefreshing = loading.isLoading
      }
    }

    state.onChange(oldState, field = { it.entries }) { entries ->
      if (entries.isEmpty()) {
        clearList()
      } else {
        setList(entries)
      }
    }

    state.onChange(oldState, field = { it.throwable }) { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }
  }
}
