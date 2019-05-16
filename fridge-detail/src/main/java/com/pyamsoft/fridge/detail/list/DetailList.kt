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

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.popinnow.android.refresh.RefreshLatch
import com.popinnow.android.refresh.newRefreshLatch
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.R.drawable
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.PickDate
import com.pyamsoft.pydroid.arch.impl.BaseUiView
import com.pyamsoft.pydroid.arch.impl.onChange
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing

abstract class DetailList protected constructor(
  parent: ViewGroup,
  private val owner: LifecycleOwner,
  private val editable: Boolean
) : BaseUiView<DetailListViewState, DetailListViewEvent>(parent) {

  final override val layout: Int = R.layout.detail_list

  final override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by boundView<RecyclerView>(R.id.detail_list)

  private var decoration: DividerItemDecoration? = null
  private var touchHelper: ItemTouchHelper? = null
  private var modelAdapter: DetailListAdapter? = null
  private var refreshLatch: RefreshLatch? = null

  @CheckResult
  internal abstract fun createDaggerComponent(
    parent: ViewGroup,
    item: FridgeItem,
    editable: Boolean
  ): DetailItemComponent

  final override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    val factory = { parent: ViewGroup, item: FridgeItem, editable: Boolean ->
      createDaggerComponent(parent, item, editable)
    }

    modelAdapter =
      DetailListAdapter(editable, factory, callback = object : DetailListAdapter.Callback {

        override fun onItemExpanded(item: FridgeItem) {
          publish(ExpandItem(item))
        }

        override fun onPickDate(
          oldItem: FridgeItem,
          year: Int,
          month: Int,
          day: Int
        ) {
          publish(PickDate(oldItem, year, month, day))
        }

      })

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    val decor = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
    recyclerView.addItemDecoration(decor)
    decoration = decor

    recyclerView.adapter = usingAdapter().apply { setHasStableIds(true) }

    refreshLatch = newRefreshLatch(owner) { isRefreshing ->
      layoutRoot.refreshing(isRefreshing)
    }

    layoutRoot.setOnRefreshListener {
      publish(ForceRefresh)
    }
    setupSwipeCallback()
  }

  private fun setupSwipeCallback() {
    val itemSwipeCallback = SimpleSwipeCallback.ItemSwipeCallback { position, direction ->
      if (direction == ItemTouchHelper.LEFT || direction == ItemTouchHelper.RIGHT) {
        if (direction == ItemTouchHelper.RIGHT) {
          archiveListItem(position)
        } else {
          archiveListItem(position)
        }
      }
    }
    val directions = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    val leftBehindDrawable =
      AppCompatResources.getDrawable(
          recyclerView.context,
          drawable.ic_delete_24dp
      )
    val leftBackground = Color.RED
    val swipeCallback = object : SimpleSwipeCallback(
        itemSwipeCallback,
        leftBehindDrawable,
        directions,
        leftBackground
    ) {

      override fun getSwipeDirs(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
      ): Int {
        if (!editable || viewHolder is DetailListAdapter.AddNewItemViewHolder) {
          return 0
        } else {
          // Don't call super here or we crash from a Reflection error
          return directions
        }
      }

    }.apply {
      withBackgroundSwipeRight(leftBackground)
      withLeaveBehindSwipeRight(leftBehindDrawable)
    }

    val helper = ItemTouchHelper(swipeCallback)
    helper.attachToRecyclerView(recyclerView)
    touchHelper = helper
  }

  final override fun onTeardown() {
    // Throws
    // recyclerView.adapter = null
    clearList()
    clearError()

    touchHelper?.attachToRecyclerView(null)
    decoration?.let { recyclerView.removeItemDecoration(it) }
    layoutRoot.setOnRefreshListener(null)

    modelAdapter = null
    touchHelper = null
    decoration = null
    refreshLatch = null
  }

  @CheckResult
  private fun usingAdapter(): DetailListAdapter {
    return requireNotNull(modelAdapter)
  }

  private fun archiveListItem(position: Int) {
    withViewHolderAt(position) { it.archiveSelf() }
  }

  private inline fun withViewHolderAt(
    position: Int,
    crossinline func: (holder: DetailListAdapter.DetailItemViewHolder) -> Unit
  ) {
    val holder: ViewHolder? = recyclerView.findViewHolderForLayoutPosition(position)
    if (holder is DetailListAdapter.DetailItemViewHolder) {
      func(holder)
    }
  }

  private fun setList(list: List<FridgeItem>) {
    usingAdapter().submitList(list)
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

  final override fun onRender(
    state: DetailListViewState,
    oldState: DetailListViewState?
  ) {
    state.onChange(oldState, field = { it.isLoading }) { loading ->
      if (loading != null) {
        requireNotNull(refreshLatch).isRefreshing = loading.isLoading
      }
    }

    state.onChange(oldState, field = { it.items }) { items ->
      if (items.isEmpty()) {
        clearList()
      } else {
        setList(items)
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
