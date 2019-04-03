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
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ModelAdapter
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.list.item.DaggerDetailItemComponent
import com.pyamsoft.fridge.detail.list.item.DetailItem
import com.pyamsoft.fridge.detail.list.item.add.AddNewListItemController
import com.pyamsoft.fridge.detail.list.item.fridge.DetailListItemController
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject
import javax.inject.Named

internal class DetailList @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  @Named("detail_editable") private val editable: Boolean,
  private val interactor: DetailListInteractor,
  private val imageLoader: ImageLoader,
  private val stateMap: MutableMap<String, Int>,
  private val theming: Theming,
  parent: ViewGroup,
  callback: DetailList.Callback
) : BaseUiView<DetailList.Callback>(parent, callback),
  DetailListItemController.Callback,
  AddNewListItemController.Callback {

  override val layout: Int = R.layout.detail_list

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.detail_list)

  private var decoration: DividerItemDecoration? = null
  private var modelAdapter: ModelAdapter<FridgeItem, DetailItem<*, *>>? = null

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    val builder = DaggerDetailItemComponent.builder()
      .interactor(interactor)
      .editable(editable)
      .entryId(entryId)
      .imageLoader(imageLoader)
      .stateMap(stateMap)
      .theming(theming)

    modelAdapter = ModelAdapter { item ->
      if (item.id().isBlank()) {
        return@ModelAdapter AddNewListItemController(
          item,
          builder,
          this
        )
      } else {
        return@ModelAdapter DetailListItemController(
          item,
          builder,
          this
        )
      }
    }

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
  }

  override fun onTeardown() {

    // Throws
    // recyclerView.adapter = null
    usingAdapter().clear()
    modelAdapter = null

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
    addNewItemIfEmpty()
  }

  private fun addNewItemIfEmpty() {
    if (usingAdapter().adapterItemCount <= 1) {
      onAddNewItem()
    }
  }

  @CheckResult
  private fun isThisEntry(item: FridgeItem): Boolean {
    return item.entryId() == entryId
  }

  fun insert(item: FridgeItem) {
    if (!updateExistingItem(item)) {
      if (isThisEntry(item)) {
        addToEndBeforeAddNew(item)
      }
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

    addNewItemIfEmpty()
  }

  override fun onAddNewItem() {
    insert(FridgeItem.create(entryId = entryId))
  }

  override fun onDelete(item: FridgeItem) {
    delete(item)
  }

  override fun onCommitError(throwable: Throwable) {
    showError(throwable)
  }

  override fun onDeleteError(throwable: Throwable) {
    showError(throwable)
  }

  override fun onOpenScanner(item: FridgeItem) {
    callback.onOpenScanner(item)
  }

  interface Callback {

    fun onRefresh()

    fun onOpenScanner(item: FridgeItem)

  }
}
