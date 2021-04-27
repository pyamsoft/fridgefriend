/*
 * Copyright 2021 Peter Kenji Yamanaka
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

import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailAddNewBinding
import com.pyamsoft.fridge.detail.snackbar.CustomSnackbar
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.loader.disposeOnDestroy
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailAddItemView @Inject internal constructor(
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ButtonEvent, DetailAddNewBinding>(
    parent
), SnackbarContainer {

    override val viewBinding = DetailAddNewBinding::inflate

    override val layoutRoot by boundView { detailAddNewRoot }

    private var filterIconLoaded: Loaded? = null

    private var addNewAnimatorCompat: ViewPropertyAnimatorCompat? = null
    private var filterAnimatorCompat: ViewPropertyAnimatorCompat? = null

    init {
        doOnInflate {
            imageLoader
                .load(R.drawable.ic_add_24dp)
                .into(binding.detailAddNewItem)
                .disposeOnDestroy(owner)

            binding.detailAddNewItem.setOnDebouncedClickListener {
                publish(DetailViewEvent.ButtonEvent.AddNew)
            }
        }

        doOnTeardown {
            binding.detailAddNewItem.setOnClickListener(null)
        }

        doOnInflate {
            binding.detailFilterItem.setOnDebouncedClickListener {
                publish(DetailViewEvent.ButtonEvent.ChangeCurrentFilter)
            }
        }

        doOnTeardown {
            binding.detailFilterItem.setOnClickListener(null)
        }

        doOnInflate {
            addNewAnimatorCompat = binding.detailAddNewItem.popShow()
        }

        doOnTeardown {
            addNewAnimatorCompat?.cancel()
            addNewAnimatorCompat = null
        }

        doOnTeardown {
            filterAnimatorCompat?.cancel()
            filterAnimatorCompat = null
        }
    }

    private fun clearFilter() {
        filterIconLoaded?.dispose()
        filterIconLoaded = null
    }

    override fun container(): CoordinatorLayout {
        return layoutRoot
    }

    override fun onRender(state: UiRender<DetailViewState>) {
        state.mapChanged { it.showing }.render(viewScope) { handleShowing(it) }
        state.mapChanged { it.listItemPresence }.render(viewScope) { handlePresence(it) }
        state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
        state.mapChanged { it.listError }.render(viewScope) { handleError(it) }
        state.mapChanged { it.undoable }.render(viewScope) { handleUndo(it) }
    }

    private fun handlePresence(presence: FridgeItem.Presence) {
        // Hide filter button for NEED
        if (presence == FridgeItem.Presence.NEED) {
            binding.detailFilterItem.isVisible = false
            filterAnimatorCompat?.cancel()
            filterAnimatorCompat = null
        } else {
            if (filterAnimatorCompat == null) {
                filterAnimatorCompat = binding.detailFilterItem.popShow()
            }
        }
    }

    private fun handleShowing(showing: DetailViewState.Showing) {
        clearFilter()
        filterIconLoaded = imageLoader.asDrawable()
            .load(
                when (showing) {
                    DetailViewState.Showing.FRESH -> R.drawable.ic_category_24
                    DetailViewState.Showing.CONSUMED -> R.drawable.ic_consumed_24dp
                    DetailViewState.Showing.SPOILED -> R.drawable.ic_spoiled_24dp
                }
            )
            .into(binding.detailFilterItem)
    }

    private fun handleBottomMargin(height: Int) {
        if (height > 0) {
            layoutRoot.updatePadding(bottom = height)
        }
    }

    private fun handleError(throwable: Throwable?) {
        if (throwable != null) {
            showListError(throwable)
        }
    }

    private fun handleUndo(undoable: DetailViewState.Undoable?) {
        if (undoable != null) {
            showUndoSnackbar(undoable)
        }
    }

    private fun showListError(throwable: Throwable) {
        CustomSnackbar.Break.bindTo(owner) {
            long(
                layoutRoot,
                throwable.message ?: "An unexpected error has occurred.",
                onHidden = { _, _ -> publish(DetailViewEvent.ButtonEvent.ClearListError) }
            )
        }
    }

    private fun showUndoSnackbar(undoable: DetailViewState.Undoable) {
        val item = undoable.item
        val canAddAgain = undoable.canQuickAdd

        val message = when {
            item.isConsumed() -> "Consumed ${item.name()}"
            item.isSpoiled() -> "${item.name()} spoiled"
            else -> "Removed ${item.name()}"
        }

        CustomSnackbar.Break.bindTo(owner) {
            long(
                layoutRoot,
                message,
                onHidden = { _, _ -> publish(DetailViewEvent.ButtonEvent.ReallyDeleteItemNoUndo) }
            ) {
                // If we have consumed/spoiled this item
                // We can offer it as 're-add'
                if ((item.isConsumed() || item.isSpoiled()) && canAddAgain) {
                    setAction1("Again") { publish(DetailViewEvent.ButtonEvent.AnotherOne(item)) }
                }

                // Restore the old item
                setAction2("Undo") { publish(DetailViewEvent.ButtonEvent.UndoDeleteItem) }
            }
        }
    }
}
