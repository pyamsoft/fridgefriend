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

package com.pyamsoft.fridge.detail.list

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject
import javax.inject.Named

internal class DetailList @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<DetailList.Callback>(parent, callback) {

  override val layout: Int = R.layout.detail_list

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.detail_list)

  private var modelAdapter: ModelAdapter<FridgeItem, DetailListItem>? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    modelAdapter = ModelAdapter { DetailListItem(it, entryId, callback) }

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    recyclerView.adapter =
      FastAdapter.with<DetailListItem, ModelAdapter<FridgeItem, *>>(usingAdapter())

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
  private fun usingAdapter(): ModelAdapter<FridgeItem, *> {
    return requireNotNull(modelAdapter)
  }

  fun beginRefresh() {
    layoutRoot.refreshing(true)
    usingAdapter().clear()
  }

  fun setList(items: List<FridgeItem>) {
    usingAdapter().add(items)
  }

  fun showError(throwable: Throwable) {
    usingAdapter().clear()

    // TODO set error text
  }

  fun finishRefresh() {
    layoutRoot.refreshing(false)

    if (usingAdapter().adapterItemCount == 0) {
      // This list is empty, add our first item
      insert(FridgeItem.create(entryId = entryId))
    }
  }

  @CheckResult
  private fun isThisEntry(item: FridgeItem): Boolean {
    return item.entryId() == entryId
  }

  fun insert(item: FridgeItem) {
    if (!updateExistingItem(item)) {
      if (isThisEntry(item)) {
        usingAdapter().add(item)
      }
    }
  }

  fun update(item: FridgeItem) {
    updateExistingItem(item)
  }

  private fun updateExistingItem(item: FridgeItem): Boolean {
    for ((index, e) in usingAdapter().models.withIndex()) {
      if (item.id() == e.id() && item.entryId() == e.entryId() && isThisEntry(item)) {
        usingAdapter().set(index, item)
        return true
      }
    }

    return false
  }

  fun delete(item: FridgeItem) {
    var index = -1
    for ((i, e) in usingAdapter().models.withIndex()) {
      if (item.id() == e.id() && item.entryId() == e.entryId() && isThisEntry(item)) {
        index = i
        break
      }
    }

    if (index >= 0) {
      usingAdapter().remove(index)
    }
  }

  interface Callback : DetailListItem.Callback {

    fun onRefresh()

  }
}
