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
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.mikepenz.fastadapter_extensions.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.DetailListAdapter.Callback
import com.pyamsoft.fridge.detail.item.DetailListItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.widget.scroll.HideOnScrollListener
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.tintWith
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

class DetailList @Inject internal constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader,
    private val owner: LifecycleOwner,
    componentCreator: DetailListItemComponentCreator
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list

    override val layoutRoot by boundView<SwipeRefreshLayout>(R.id.detail_swipe_refresh)

    private val recyclerView by boundView<RecyclerView>(R.id.detail_list)

    private var touchHelper: ItemTouchHelper? = null
    private var modelAdapter: DetailListAdapter? = null

    init {
        doOnInflate {
            recyclerView.layoutManager = LinearLayoutManager(recyclerView.context).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
            }
        }

        doOnInflate {
            modelAdapter = DetailListAdapter(
                editable = false,
                owner = owner,
                componentCreator = componentCreator,
                callback = object : Callback {
                    override fun onItemExpanded(index: Int) {
                        publish(DetailViewEvent.ExpandItem(index))
                    }

                    override fun onPresenceChange(index: Int) {
                        publish(DetailViewEvent.ChangePresence(index))
                    }
                })
            recyclerView.adapter = usingAdapter().apply { setHasStableIds(true) }
        }

        doOnInflate {
            val scrollListener = HideOnScrollListener.create(startVisible = true, distance = 24) {
                publish(DetailViewEvent.ScrollActionVisibilityChange(it))
            }
            recyclerView.addOnScrollListener(scrollListener)

            doOnTeardown {
                recyclerView.removeOnScrollListener(scrollListener)
            }
        }

        doOnInflate {
            layoutRoot.setOnRefreshListener { publish(DetailViewEvent.ForceRefresh) }
        }

        doOnTeardown {
            // Throws
            // recyclerView.adapter = null
            clearList()

            touchHelper?.attachToRecyclerView(null)
            layoutRoot.setOnRefreshListener(null)

            modelAdapter = null
            touchHelper = null
        }
    }

    private fun setupSwipeCallback(showArchived: Boolean, listItemPresence: FridgeItem.Presence) {
        val swipeAwayDeletes = !showArchived && listItemPresence == NEED
        val swipeAwayRestores = showArchived && listItemPresence == HAVE

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
                if (swipeAwayDeletes) {
                    deleteListItem(position)
                } else if (swipeAwayRestores) {
                    if (direction == consumeSwipeDirection) {
                        // Restore from archive
                        restoreListItem(position)
                    } else {
                        // Delete forever
                        deleteListItem(position)
                    }
                } else {
                    if (direction == consumeSwipeDirection) {
                        consumeListItem(position)
                    } else {
                        spoilListItem(position)
                    }
                }
            }
        }
        val leftBehindDrawable = imageLoader.immediate(
            when {
                swipeAwayDeletes -> R.drawable.ic_delete_24dp
                swipeAwayRestores -> R.drawable.ic_delete_24dp
                else -> R.drawable.ic_spoiled_24dp
            }
        ).tintWith(layoutRoot.context, R.color.white)

        val directions = consumeSwipeDirection or spoilSwipeDirection
        val swipeCallback = object : SimpleSwipeCallback(
            itemSwipeCallback,
            leftBehindDrawable,
            directions,
            Color.TRANSPARENT
        ) {

            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: ViewHolder
            ): Int {
                // Can't swipe non item view holders
                return if (viewHolder is DetailItemViewHolder) directions else 0
            }
        }.apply {
            val rightBehindDrawable = imageLoader.immediate(
                when {
                    swipeAwayDeletes -> R.drawable.ic_delete_24dp
                    swipeAwayRestores -> R.drawable.ic_code_24dp
                    else -> R.drawable.ic_consumed_24dp
                }
            ).tintWith(layoutRoot.context, R.color.white)
            withBackgroundSwipeRight(Color.TRANSPARENT)
            withLeaveBehindSwipeRight(rightBehindDrawable)
        }

        // Detach any existing helper from the recyclerview
        touchHelper?.attachToRecyclerView(null)

        // Attach new helper
        val helper = ItemTouchHelper(swipeCallback)
        helper.attachToRecyclerView(recyclerView)
        touchHelper = helper
    }

    @CheckResult
    private fun usingAdapter(): DetailListAdapter {
        return requireNotNull(modelAdapter)
    }

    private fun restoreListItem(position: Int) {
        publish(DetailViewEvent.Restore(position))
    }

    private fun deleteListItem(position: Int) {
        publish(DetailViewEvent.Delete(position))
    }

    private fun consumeListItem(position: Int) {
        publish(DetailViewEvent.Consume(position))
    }

    private fun spoilListItem(position: Int) {
        publish(DetailViewEvent.Spoil(position))
    }

    private fun setList(
        list: List<FridgeItem>,
        expirationRange: Int,
        sameDayExpired: Boolean
    ) {
        usingAdapter().submitList(list.map {
            DetailListItemViewState(
                it,
                expirationRange,
                sameDayExpired
            )
        })
    }

    private fun clearList() {
        usingAdapter().submitList(null)
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

    private fun showUndoSnackbar(undoable: FridgeItem) {
        Snackbreak.bindTo(owner, "undo") {
            short(layoutRoot, "Removed ${undoable.name()}", onHidden = { _, _ ->
                // Once hidden this will clear out the stored undoable
                //
                // If the undoable was restored before this point, this is basically a no-op
                publish(DetailViewEvent.ReallyDeleteNoUndo(undoable))
            }) {
                // Restore the old item
                setAction("Undo") { publish(DetailViewEvent.UndoDelete(undoable)) }
            }
        }
    }

    private fun clearUndoSnackbar() {
        Snackbreak.bindTo(owner, "undo") {
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
            when {
                items.isEmpty() -> clearList()
                else -> setList(items, state.expirationRange, state.isSameDayExpired)
            }
        }

        state.listError.let { throwable ->
            if (throwable == null) {
                clearListError()
            } else {
                showListError(throwable)
            }
        }

        state.undoableItem.let { undoable ->
            if (undoable == null) {
                clearUndoSnackbar()
            } else {
                showUndoSnackbar(undoable)
            }
        }

        setupSwipeCallback(state.showArchived, state.listItemPresence)
    }
}
