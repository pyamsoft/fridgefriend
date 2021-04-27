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

package com.pyamsoft.fridge.search

import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.snackbar.CustomSnackbar
import com.pyamsoft.fridge.search.databinding.SearchFilterBinding
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class SearchFilter @Inject internal constructor(
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
) : BaseUiView<DetailViewState, SearchViewEvent, SearchFilterBinding>(parent),
    SnackbarContainer {

    override val viewBinding = SearchFilterBinding::inflate

    override val layoutRoot by boundView { searchFilterRoot }

    private var filterIconLoaded: Loaded? = null

    init {
        doOnInflate {
            binding.searchFilter.setOnDebouncedClickListener {
                publish(SearchViewEvent.ChangeCurrentFilter)
            }
        }

        doOnTeardown {
            binding.searchFilter.setOnClickListener(null)
        }

        doOnInflate {
            val animator = binding.searchFilter.popShow()
            doOnTeardown { animator.cancel() }
        }

        doOnTeardown {
            clearFilter()
        }
    }

    private fun clearFilter() {
        filterIconLoaded?.dispose()
        filterIconLoaded = null
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
            .into(binding.searchFilter)
    }

    override fun container(): CoordinatorLayout {
        return layoutRoot
    }

    override fun onRender(state: UiRender<DetailViewState>) {
        state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
        state.mapChanged { it.undoable }.render(viewScope) { handleUndo(it) }
        state.mapChanged { it.showing }.render(viewScope) { handleShowing(it) }
    }

    private fun handleBottomMargin(height: Int) {
        if (height > 0) {
            layoutRoot.updatePadding(bottom = height)
        }
    }

    private fun handleUndo(undoable: DetailViewState.Undoable?) {
        if (undoable != null) {
            showUndoSnackbar(undoable)
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
                onHidden = { _, _ -> publish(SearchViewEvent.ReallyDeleteItemNoUndo) }
            ) {
                // If we have consumed/spoiled this item
                // We can offer it as 're-add'
                if ((item.isConsumed() || item.isSpoiled()) && canAddAgain) {
                    setAction1("Again") { publish(SearchViewEvent.AnotherOne(item)) }
                }

                // Restore the old item
                setAction2("Undo") { publish(SearchViewEvent.UndoDeleteItem) }
            }
        }
    }
}
