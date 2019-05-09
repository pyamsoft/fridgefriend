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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.R.drawable
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DaggerDetailItemComponent
import com.pyamsoft.fridge.detail.list.DetailList.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.refreshing

internal abstract class DetailList protected constructor(
  parent: ViewGroup,
  callback: Callback,
  private val interactor: CreationListInteractor,
  private val imageLoader: ImageLoader,
  private val theming: Theming,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
  private val editable: Boolean
) : BaseUiView<Callback>(parent, callback),
    DetailListAdapter.Callback {

  final override val layout: Int = R.layout.detail_list

  final override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by boundView<RecyclerView>(R.id.detail_list)

  private var decoration: DividerItemDecoration? = null
  private var touchHelper: ItemTouchHelper? = null
  private var modelAdapter: DetailListAdapter? = null

  final override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    val factory = { parent: ViewGroup, item: FridgeItem, editable: Boolean ->
      DaggerDetailItemComponent.factory()
          .create(parent, item, editable, imageLoader, theming, interactor, fakeRealtime)
    }

    modelAdapter =
      DetailListAdapter(editable, factory, callback = this)

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    val decor = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
    recyclerView.addItemDecoration(decor)
    decoration = decor

    recyclerView.adapter = usingAdapter().apply { setHasStableIds(true) }

    layoutRoot.setOnRefreshListener {
      callback.onRefresh()
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
    modelAdapter = null

    touchHelper?.attachToRecyclerView(null)
    touchHelper = null

    decoration?.let { recyclerView.removeItemDecoration(it) }
    decoration = null

    layoutRoot.setOnRefreshListener(null)
  }

  @CheckResult
  private fun usingAdapter(): DetailListAdapter {
    return requireNotNull(modelAdapter)
  }

  private fun archiveListItem(position: Int) {
    withViewHolderAt(position) { it.archiveSelf() }
  }

  protected fun focusItem(position: Int) {
    withViewHolderAt(position) { it.focus() }
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

  fun beginRefresh() {
    layoutRoot.refreshing(true)
  }

  fun setList(list: List<FridgeItem>) {
    usingAdapter().submitList(list)
  }

  fun clearList() {
    usingAdapter().submitList(null)
  }

  fun showError(throwable: Throwable) {
    // TODO set error text
  }

  fun clearError() {
    // TODO clear error
  }

  fun finishRefresh() {
    layoutRoot.refreshing(false)
  }

  final override fun onItemExpanded(item: FridgeItem) {
    callback.onExpandItem(item)
  }

  interface Callback {

    fun onRefresh()

    fun onExpandItem(item: FridgeItem)

  }
}
