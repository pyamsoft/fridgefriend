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

package com.pyamsoft.fridge.detail

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
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailList.Callback
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DaggerDetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItem
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemController
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.refreshing
import timber.log.Timber

internal abstract class DetailList protected constructor(
  private val interactor: CreationListInteractor,
  private val imageLoader: ImageLoader,
  private val stateMap: MutableMap<String, Int>,
  private val theming: Theming,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback),
  DetailListItemController.Callback {

  final override val layout: Int = R.layout.detail_list

  final override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.detail_list)

  private var decoration: DividerItemDecoration? = null
  private var touchHelper: ItemTouchHelper? = null
  private var modelAdapter: ModelAdapter<FridgeItem, DetailItem<*, *>>? = null

  @CheckResult
  protected abstract fun createListItem(
    item: FridgeItem,
    builder: DetailItemComponent.Builder
  ): DetailItem<*, *>

  protected abstract fun onListEmpty()

  final override fun onInflated(view: View, savedInstanceState: Bundle?) {
    val builder = DaggerDetailItemComponent.builder()
      .interactor(interactor)
      .imageLoader(imageLoader)
      .stateMap(stateMap)
      .theming(theming)

    modelAdapter = ModelAdapter { createListItem(it, builder) }

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    val decor = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
    recyclerView.addItemDecoration(decor)
    decoration = decor

    recyclerView.adapter =
      FastAdapter.with<DetailItem<*, *>, ModelAdapter<FridgeItem, *>>(usingAdapter()).apply {
        setHasStableIds(true)
      }

    layoutRoot.setOnRefreshListener {
      callback.onRefresh()
    }
    setupSwipeCallback()
  }

  private fun setupSwipeCallback() {
    val leftBehindDrawable =
      AppCompatResources.getDrawable(recyclerView.context, R.drawable.ic_delete_24dp)
    val itemSwipeCallback = SimpleSwipeCallback.ItemSwipeCallback { position, direction ->
      Timber.d("Item swiped: $position ${if (direction == ItemTouchHelper.LEFT) "LEFT" else "RIGHT"}")
      deleteListItem(position)
    }
    val background = Color.RED
    val swipeCallback = object : SimpleSwipeCallback(
      itemSwipeCallback,
      leftBehindDrawable,
      ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
      background
    ) {

      override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: ViewHolder): Int {
        val item = FastAdapter.getHolderAdapterItem<IItem<*, *>>(viewHolder)
        if (item is DetailItem<*, *>) {
          if (item.canSwipe()) {
            return super.getMovementFlags(recyclerView, viewHolder)
          } else {
            return 0
          }
        }

        return super.getMovementFlags(recyclerView, viewHolder)
      }
    }.apply {
      withBackgroundSwipeRight(background)
      withLeaveBehindSwipeRight(leftBehindDrawable)
    }

    val helper = ItemTouchHelper(swipeCallback)
    helper.attachToRecyclerView(recyclerView)
    touchHelper = helper
  }

  private fun deleteListItem(position: Int) {
    val holder: RecyclerView.ViewHolder? = recyclerView.findViewHolderForLayoutPosition(position)
    if (holder is DetailListItemController.ViewHolder) {
      holder.deleteSelf(usingAdapter().models[holder.adapterPosition])
    }
  }

  final override fun onTeardown() {
    // Throws
    // recyclerView.adapter = null
    usingAdapter().clear()
    modelAdapter = null

    touchHelper?.attachToRecyclerView(null)
    touchHelper = null

    decoration?.let { recyclerView.removeItemDecoration(it) }
    decoration = null

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
    usingAdapter().add(FridgeItem.empty())

    fixListIfEmpty()
  }

  private fun fixListIfEmpty() {
    if (usingAdapter().adapterItemCount <= 1) {
      onListEmpty()
    }
  }

  fun insert(item: FridgeItem) {
    if (!updateExistingItem(item)) {
      addToEndBeforeAddNew(item)
    }
  }

  private fun addToEndBeforeAddNew(item: FridgeItem) {
    var index = -1
    for ((i, e) in usingAdapter().models.withIndex()) {
      if (e.id().isBlank()) {
        index = i
        break
      }
    }

    when {
      index == 0 -> usingAdapter().add(0, item)
      index > 0 -> usingAdapter().add(index, item)
      else -> usingAdapter().add(item)
    }
  }

  fun update(item: FridgeItem) {
    updateExistingItem(item)
  }

  private fun updateExistingItem(item: FridgeItem): Boolean {
    for ((index, e) in usingAdapter().models.withIndex()) {
      if (item.id() == e.id() && item.entryId() == e.entryId()) {
        usingAdapter().set(index, item)
        return true
      }
    }

    return false
  }

  fun delete(item: FridgeItem) {
    var index = -1
    for ((i, e) in usingAdapter().models.withIndex()) {
      if (item.id() == e.id() && item.entryId() == e.entryId()) {
        index = i
        break
      }
    }

    if (index >= 0) {
      usingAdapter().remove(index)
    }

    fixListIfEmpty()
  }

  final override fun onFakeDelete(item: FridgeItem) {
    delete(item)
  }

  final override fun onCommitError(throwable: Throwable) {
    showError(throwable)
  }

  final override fun onDeleteError(throwable: Throwable) {
    showError(throwable)
  }

  interface Callback {

    fun onRefresh()

  }
}
