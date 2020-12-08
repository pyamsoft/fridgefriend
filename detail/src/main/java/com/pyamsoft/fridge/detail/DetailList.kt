/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.detail

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.swipe.SimpleSwipeCallback
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.HAVE
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.databinding.DetailListBinding
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.fridge.detail.item.DetailListAdapter
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.refreshing
import com.pyamsoft.pydroid.ui.util.removeAllItemDecorations
import com.pyamsoft.pydroid.util.asDp
import com.pyamsoft.pydroid.util.tintWith
import io.cabriole.decorator.LinearBoundsMarginDecoration
import io.cabriole.decorator.LinearMarginDecoration
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import timber.log.Timber
import javax.inject.Inject
import com.pyamsoft.fridge.ui.R as R2
import com.pyamsoft.pydroid.ui.R as R3

class DetailList @Inject internal constructor(
    private val imageLoader: ImageLoader,
    private val theming: ThemeProvider,
    parent: ViewGroup,
    factory: DetailItemComponent.Factory,
) : BaseUiView<DetailViewState, DetailViewEvent, DetailListBinding>(parent) {

    override val viewBinding = DetailListBinding::inflate

    override val layoutRoot by boundView { detailSwipeRefresh }

    private var touchHelper: ItemTouchHelper? = null
    private var modelAdapter: DetailListAdapter? = null

    private var bottomMarginDecoration: RecyclerView.ItemDecoration? = null

    private var lastScrollPosition = 0

    init {
        doOnInflate {
            binding.detailList.layoutManager =
                LinearLayoutManager(binding.detailList.context).apply {
                    isItemPrefetchEnabled = true
                    initialPrefetchItemCount = 3
                }
        }

        doOnInflate {
            modelAdapter = DetailListAdapter(
                factory = factory,
                callback = object : DetailListAdapter.Callback {

                    override fun onIncreaseCount(index: Int) {
                        publish(DetailViewEvent.IncreaseItemCount(itemAtIndex(index)))
                    }

                    override fun onDecreaseCount(index: Int) {
                        publish(DetailViewEvent.DecreaseItemCount(itemAtIndex(index)))
                    }

                    override fun onItemExpanded(index: Int) {
                        publish(DetailViewEvent.ExpandItem(itemAtIndex(index)))
                    }

                    override fun onPresenceChange(index: Int) {
                        publish(DetailViewEvent.ChangeItemPresence(itemAtIndex(index)))
                    }
                })
            binding.detailList.adapter = modelAdapter
        }

        doOnInflate {
            // Fast Scroll
            FastScrollerBuilder(binding.detailList)
                .useMd2Style()
                .setPopupTextProvider(usingAdapter())
                .build()
        }

        doOnInflate {
            binding.detailSwipeRefresh.setOnRefreshListener { publish(DetailViewEvent.ForceRefresh) }
        }

        doOnInflate { savedInstanceState ->
            savedInstanceState.useIfAvailable<Int>(LAST_SCROLL_POSITION) { position ->
                Timber.d("Last scroll position saved at: $position")
                lastScrollPosition = position
            }
        }

        doOnSaveState { outState ->
            val manager = binding.detailList.layoutManager
            if (manager is LinearLayoutManager) {
                val position = manager.findFirstVisibleItemPosition()
                if (position > 0) {
                    outState.put(LAST_SCROLL_POSITION, position)
                    return@doOnSaveState
                }
            }

            outState.remove(LAST_SCROLL_POSITION)
        }

        doOnInflate {
            val margin = 16.asDp(binding.detailList.context)

            // Standard margin on all items
            LinearMarginDecoration.create(margin = margin).apply {
                binding.detailList.addItemDecoration(this)
            }
        }

        doOnTeardown {
            removeBottomMargin()
            binding.detailList.removeAllItemDecorations()
        }

        doOnTeardown {
            modelAdapter?.submitList(null)
            binding.detailList.adapter = null

            touchHelper?.attachToRecyclerView(null)
            binding.detailSwipeRefresh.setOnRefreshListener(null)

            modelAdapter = null
            touchHelper = null
        }
    }

    private fun removeBottomMargin() {
        bottomMarginDecoration?.also { binding.detailList.removeItemDecoration(it) }
        bottomMarginDecoration = null
    }

    @CheckResult
    private fun itemAtIndex(index: Int): FridgeItem {
        return usingAdapter().currentList[index].item
    }

    private fun setupSwipeCallback(state: DetailViewState) {
        val isFresh = state.showing == DetailViewState.Showing.FRESH
        val swipeAwayDeletes = isFresh && state.listItemPresence == NEED
        val swipeAwayRestores = !isFresh && state.listItemPresence == HAVE

        val consumeSwipeDirection = ItemTouchHelper.RIGHT
        val spoilSwipeDirection = ItemTouchHelper.LEFT
        val directions = consumeSwipeDirection or spoilSwipeDirection

        applySwipeCallback(
            directions,
            swipeAwayDeletes,
            swipeAwayRestores
        ) { position, direction ->
            val holder = binding.detailList.findViewHolderForAdapterPosition(position)
            if (holder == null) {
                Timber.w("ViewHolder is null, cannot respond to swipe")
                return@applySwipeCallback
            }
            if (holder !is DetailItemViewHolder) {
                Timber.w("ViewHolder is not DetailItemViewHolder, cannot respond to swipe")
                return@applySwipeCallback
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
    }

    @CheckResult
    private fun Drawable.themeIcon(): Drawable {
        val color = if (theming.isDarkTheme()) R3.color.white else R3.color.black
        return this.tintWith(layoutRoot.context, color)
    }

    private inline fun applySwipeCallback(
        directions: Int,
        swipeAwayDeletes: Boolean,
        swipeAwayRestores: Boolean,
        crossinline itemSwipeCallback: (position: Int, directions: Int) -> Unit,
    ) {
        val leftBehindDrawable = imageLoader.load(
            when {
                swipeAwayDeletes -> R2.drawable.ic_delete_24dp
                swipeAwayRestores -> R2.drawable.ic_delete_24dp
                else -> R2.drawable.ic_spoiled_24dp
            }
        )
            .mutate { it.themeIcon() }
            .immediate()

        val swipeCallback = SimpleSwipeCallback(
            object : SimpleSwipeCallback.ItemSwipeCallback {

                override fun itemSwiped(position: Int, direction: Int) {
                    itemSwipeCallback(position, direction)
                }
            },
            requireNotNull(leftBehindDrawable),
            directions,
            Color.TRANSPARENT
        ).apply {
            val rightBehindDrawable = imageLoader.load(
                when {
                    swipeAwayDeletes -> R2.drawable.ic_delete_24dp
                    swipeAwayRestores -> R.drawable.ic_restore_from_trash_24
                    else -> R2.drawable.ic_consumed_24dp
                }
            )
                .mutate { it.themeIcon() }
                .immediate()
            withBackgroundSwipeRight(Color.TRANSPARENT)
            withLeaveBehindSwipeRight(requireNotNull(rightBehindDrawable))
        }

        // Detach any existing helper from the recyclerview
        touchHelper?.attachToRecyclerView(null)

        // Attach new helper
        val helper = ItemTouchHelper(swipeCallback).apply {
            attachToRecyclerView(binding.detailList)
        }

        // Set helper for cleanup later
        touchHelper = helper
    }

    @CheckResult
    private fun usingAdapter(): DetailListAdapter {
        return requireNotNull(modelAdapter)
    }

    private fun restoreListItem(position: Int) {
        publish(DetailViewEvent.RestoreItem(itemAtIndex(position)))
    }

    private fun deleteListItem(position: Int) {
        publish(DetailViewEvent.DeleteItem(itemAtIndex(position)))
    }

    private fun consumeListItem(position: Int) {
        publish(DetailViewEvent.ConsumeItem(itemAtIndex(position)))
    }

    private fun spoilListItem(position: Int) {
        publish(DetailViewEvent.SpoilItem(itemAtIndex(position)))
    }

    private fun setList(
        list: List<FridgeItem>,
        expirationRange: DetailViewState.ExpirationRange?,
        sameDayExpired: DetailViewState.IsSameDayExpired?,
    ) {
        val data = list.map { DetailItemViewState(it, expirationRange, sameDayExpired) }
        usingAdapter().submitList(data)
    }

    private fun clearList() {
        usingAdapter().submitList(null)
    }

    private fun handleLoading(loading: Boolean) {
        binding.detailSwipeRefresh.refreshing(loading)
    }

    private fun handleList(state: DetailViewState) {
        state.displayedItems.let { items ->
            when {
                items.isEmpty() -> clearList()
                else -> setList(
                    items,
                    state.expirationRange,
                    state.isSameDayExpired
                )
            }
        }
    }

    private fun handleBottomMargin(height: Int) {
        removeBottomMargin()
        if (height > 0) {
            // The bottom has additional space to fit the FAB
            val fabSpacing = 72.asDp(binding.detailList.context)
            LinearBoundsMarginDecoration(bottomMargin = fabSpacing + height).apply {
                binding.detailList.addItemDecoration(this)
                bottomMarginDecoration = this
            }
        }
    }

    private fun restoreLastScrollPosition(state: DetailViewState) {
        if (lastScrollPosition > 0) {
            if (!state.isLoading && state.displayedItems.isNotEmpty()) {
                val position = lastScrollPosition
                lastScrollPosition = 0

                Timber.d("Restoring visual scroll position: $position")
                binding.detailList.scrollToPosition(position)
            }
        }
    }

    override fun onRender(state: UiRender<DetailViewState>) {
        state.distinctBy { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
        state.distinctBy { it.isLoading }.render(viewScope) { handleLoading(it) }
        state.render(viewScope) { s ->
            handleList(s)
            setupSwipeCallback(s)
            restoreLastScrollPosition(s)
        }
    }

    companion object {
        private const val LAST_SCROLL_POSITION = "detail_last_scroll_position"
    }
}
