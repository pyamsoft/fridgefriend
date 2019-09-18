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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.detail.DetailListAdapter.Callback
import com.pyamsoft.fridge.detail.DetailViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.DetailViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.DetailViewEvent.PickDate
import com.pyamsoft.fridge.detail.item.DateSelectPayload
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

class DetailList @Inject internal constructor(
    parent: ViewGroup,
    private val interactor: DetailInteractor,
    private val imageLoader: ImageLoader,
    private val theming: Theming,
    private val realtime: FridgeItemRealtime,
    private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
    private val dateSelectBus: EventBus<DateSelectPayload>,
    private val listItemPresence: FridgeItem.Presence,
    private val owner: LifecycleOwner
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list

    override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

    private val recyclerView by boundView<RecyclerView>(R.id.detail_list)

    private var decoration: DividerItemDecoration? = null
    private var touchHelper: ItemTouchHelper? = null
    private var modelAdapter: DetailListAdapter? = null

    override fun onInflated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        val component = DaggerDetailListComponent.factory()
            .create(
                imageLoader, theming, interactor,
                realtime, fakeRealtime, dateSelectBus, listItemPresence
            ).plusItemComponent()

        val injectComponent = { parent: ViewGroup, item: FridgeItem, editable: Boolean ->
            component.create(parent, item, editable)
        }

        modelAdapter =
            DetailListAdapter(
                editable = false,
                injectComponent = injectComponent,
                callback = object : Callback {

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

        layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
            val offset = 8.toDp(v.context)
            val toolbarTopMargin = padding.top + insets.systemWindowInsetTop + offset
            layoutRoot.setProgressViewOffset(
                false,
                toolbarTopMargin,
                toolbarTopMargin * 3
            )
        }
        layoutRoot.setOnRefreshListener { publish(ForceRefresh) }
        setupSwipeCallback()
    }

    private fun setupSwipeCallback() {
        val consumeSwipeDirection = ItemTouchHelper.RIGHT
        val spoilSwipeDirection = ItemTouchHelper.LEFT
        val itemSwipeCallback = SimpleSwipeCallback.ItemSwipeCallback { position, direction ->
            val holder = recyclerView.findViewHolderForAdapterPosition(position)
            if (holder == null) {
                Timber.w("ViewHolder is null, cannot respond to swipe")
                return@ItemSwipeCallback
            }
            if (holder !is DetailItemViewHolder) {
                Timber.w("ViewHolder is not DetailItemViewHolder, cannot respond to swipe")
                return@ItemSwipeCallback
            }

            if (direction == consumeSwipeDirection || direction == spoilSwipeDirection) {
                if (direction == consumeSwipeDirection) {
                    consumeListItem(position)
                } else {
                    spoilListItem(position)
                }
            }
        }
        val leftBehindDrawable = imageLoader.immediate(R.drawable.ic_spoiled_24dp)
        val leftBackground = Color.RED

        val directions = consumeSwipeDirection or spoilSwipeDirection
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
                // Can't swipe non item view holders
                return if (viewHolder is DetailItemViewHolder) directions else 0
            }
        }.apply {
            val rightBehindDrawable = imageLoader.immediate(R.drawable.ic_consumed_24dp)
            val rightBackground = Color.GREEN
            withBackgroundSwipeRight(rightBackground)
            withLeaveBehindSwipeRight(rightBehindDrawable)
        }

        val helper = ItemTouchHelper(swipeCallback)
        helper.attachToRecyclerView(recyclerView)
        touchHelper = helper
    }

    override fun onTeardown() {
        // Throws
        // recyclerView.adapter = null
        clearList()

        touchHelper?.attachToRecyclerView(null)
        decoration?.let { recyclerView.removeItemDecoration(it) }
        layoutRoot.setOnRefreshListener(null)

        modelAdapter = null
        touchHelper = null
        decoration = null
    }

    @CheckResult
    private fun usingAdapter(): DetailListAdapter {
        return requireNotNull(modelAdapter)
    }

    private fun consumeListItem(position: Int) {
        withViewHolderAt(position) { it.consume() }
    }

    private fun spoilListItem(position: Int) {
        withViewHolderAt(position) { it.spoil() }
    }

    private inline fun withViewHolderAt(
        position: Int,
        crossinline func: (holder: DetailItemViewHolder) -> Unit
    ) {
        val holder: ViewHolder? = recyclerView.findViewHolderForLayoutPosition(position)
        if (holder is DetailItemViewHolder) {
            func(holder)
        }
    }

    private fun setList(list: List<FridgeItem>) {
        usingAdapter().submitList(list)
    }

    private fun clearList() {
        usingAdapter().submitList(null)
    }

    private fun showNameUpdateError(throwable: Throwable) {
        Snackbreak.bindTo(owner, "name") {
            make(layoutRoot, throwable.message ?: "An unexpected error has occurred.")
        }
    }

    private fun clearNameUpdateError() {
        Snackbreak.bindTo(owner, "name") {
            dismiss()
        }
    }

    private fun showListError(throwable: Throwable) {
        Snackbreak.bindTo(owner, "list") {
            make(layoutRoot, throwable.message ?: "An unexpected error has occurred.")
        }
    }

    private fun clearListError() {
        Snackbreak.bindTo(owner, "list") {
            dismiss()
        }
    }

    override fun onRender(
        state: DetailViewState,
        savedState: UiSavedState
    ) {
        state.isLoading.let { loading ->
            if (loading != null) {
                layoutRoot.refreshing(loading.isLoading)
            }
        }

        state.items.let { items ->
            if (items.isEmpty()) {
                clearList()
            } else {
                setList(items)
            }
        }

        state.listError.let { throwable ->
            if (throwable == null) {
                clearListError()
            } else {
                showListError(throwable)
            }
        }

        state.nameUpdateError.let { throwable ->
            if (throwable == null) {
                clearNameUpdateError()
            } else {
                showNameUpdateError(throwable)
            }
        }
    }
}
