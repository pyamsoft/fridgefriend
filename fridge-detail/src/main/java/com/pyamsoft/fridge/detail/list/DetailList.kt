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
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.mikepenz.fastadapter.commons.utils.FastAdapterDiffUtil
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.core.DataClassDiffCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.R.drawable
import com.pyamsoft.fridge.detail.create.list.CreationListInteractor
import com.pyamsoft.fridge.detail.item.DaggerDetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItem
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.fridge.DetailListItemController
import com.pyamsoft.fridge.detail.list.DetailList.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.refreshing
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.dimming.TintPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

internal abstract class DetailList protected constructor(
  private val interactor: CreationListInteractor,
  private val imageLoader: ImageLoader,
  private val theming: Theming,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback),
    DetailListItemController.Callback {

  final override val layout: Int = R.layout.detail_list

  final override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by boundView<InboxRecyclerView>(R.id.detail_list)
  private val expandablePage by boundView<ExpandablePageLayout>(R.id.detail_expand)

  private var decoration: DividerItemDecoration? = null
  private var touchHelper: ItemTouchHelper? = null
  private var modelAdapter: ModelAdapter<FridgeItem, DetailItem<*, *>>? = null

  @CheckResult
  protected abstract fun createListItem(
    item: FridgeItem,
    factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ): DetailItem<*, *>

  final override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    val factory = { parent: ViewGroup, item: FridgeItem, editable: Boolean ->
      DaggerDetailItemComponent.factory()
          .create(parent, item, editable, imageLoader, theming, interactor, fakeRealtime)
    }

    modelAdapter = ModelAdapter { createListItem(it, factory) }

    recyclerView.layoutManager = LinearLayoutManager(view.context).apply {
      isItemPrefetchEnabled = true
      initialPrefetchItemCount = 3
    }

    val decor = DividerItemDecoration(view.context, DividerItemDecoration.VERTICAL)
    recyclerView.addItemDecoration(decor)
    decoration = decor

    recyclerView.setExpandablePage(expandablePage)
    recyclerView.tintPainter = TintPainter.uncoveredArea(
        color = if (theming.isDarkTheme()) Color.BLACK else Color.WHITE,
        opacity = 0.65F
    )

    recyclerView.adapter =
      FastAdapter.with<DetailItem<*, *>, ModelAdapter<FridgeItem, *>>(usingAdapter())
          .apply {
            setHasStableIds(true)
          }

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
          deleteListItem(position)
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
    val rightBehindDrawable =
      AppCompatResources.getDrawable(
          recyclerView.context,
          drawable.ic_archive_24dp
      )
    val rightBackground = Color.GREEN
    val swipeCallback = object : SimpleSwipeCallback(
        itemSwipeCallback,
        leftBehindDrawable,
        directions,
        leftBackground
    ) {

      override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: ViewHolder
      ): Int {
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
      withBackgroundSwipeRight(rightBackground)
      withLeaveBehindSwipeRight(rightBehindDrawable)
    }

    val helper = ItemTouchHelper(swipeCallback)
    helper.attachToRecyclerView(recyclerView)
    touchHelper = helper
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
  private fun usingAdapter(): ModelAdapter<FridgeItem, DetailItem<*, *>> {
    return requireNotNull(modelAdapter)
  }

  private fun deleteListItem(position: Int) {
    withViewHolderAt(position) { it.deleteSelf(usingAdapter().models[it.adapterPosition]) }
  }

  private fun archiveListItem(position: Int) {
    withViewHolderAt(position) { it.archiveSelf(usingAdapter().models[it.adapterPosition]) }
  }

  private inline fun withViewHolderAt(
    position: Int,
    crossinline func: (holder: DetailListItemController.ViewHolder) -> Unit
  ) {
    val holder: ViewHolder? = recyclerView.findViewHolderForLayoutPosition(position)
    if (holder is DetailListItemController.ViewHolder) {
      func(holder)
    }
  }

  @CheckResult
  protected fun getItemCount(): Int {
    return usingAdapter().adapterItems.filter {
      it.getModel()
          .id()
          .isNotBlank()
    }
        .size
  }

  protected fun focusItem(position: Int) {
    withViewHolderAt(position) { it.focus() }
  }

  fun beginRefresh() {
    layoutRoot.refreshing(true)
  }

  fun setList(list: List<FridgeItem>) {
    val adapter = usingAdapter()
    val items = list.map { adapter.intercept(it) }
    FastAdapterDiffUtil.set(adapter, items, DataClassDiffCallback.create(), true)
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
  }

  final override fun onItemUpdated(item: FridgeItem) {
    val adapter = usingAdapter()
    val models = adapter.models
    val copyOfModels = models.toMutableList()
    for ((index, model) in models.withIndex()) {
      if (model.id() == item.id()) {
        copyOfModels[index] = item
        break
      }
    }

    // Set the new list but do not notify a UI update
    adapter.set(copyOfModels, false) { _, _, _, _ -> false }
  }

  interface Callback {

    fun onRefresh()

  }
}
