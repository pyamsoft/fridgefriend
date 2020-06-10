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
 *
 */

package com.pyamsoft.fridge.detail

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.AddNewBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailAddItemView @Inject internal constructor(
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
    listItemPresence: FridgeItem.Presence
) : BaseUiView<DetailViewState, DetailViewEvent, AddNewBinding>(parent) {

    override val viewBinding = AddNewBinding::inflate

    override val layoutRoot by boundView { detailAddNewRoot }

    private var addNewIconLoaded: Loaded? = null
    private var filterIconLoaded: Loaded? = null

    init {
        doOnInflate {
            addNewIconLoaded = imageLoader
                .load(R.drawable.ic_add_24dp)
                .into(binding.detailAddNewItem)

            binding.detailAddNewItem.setOnDebouncedClickListener {
                publish(DetailViewEvent.AddNewItemEvent)
            }
        }

        doOnTeardown {
            disposeAddNewLoaded()
            binding.detailAddNewItem.setOnClickListener(null)
        }

        doOnInflate {
            binding.detailFilterItem.setOnDebouncedClickListener {
                publish(DetailViewEvent.ToggleArchiveVisibility)
            }
        }

        doOnTeardown {
            disposeFilterLoaded()
            binding.detailFilterItem.setOnClickListener(null)
        }

        doOnInflate {
            binding.detailAddNewItem.popShow().withEndAction {
                if (listItemPresence == FridgeItem.Presence.HAVE) {
                    binding.detailFilterItem.popShow().apply { doOnTeardown { cancel() } }
                } else {
                    binding.detailFilterItem.isVisible = false
                }
            }.apply { doOnTeardown { cancel() } }
        }
    }

    private fun disposeFilterLoaded() {
        filterIconLoaded?.dispose()
        filterIconLoaded = null
    }

    private fun disposeAddNewLoaded() {
        addNewIconLoaded?.dispose()
        addNewIconLoaded = null
    }

    override fun onRender(state: DetailViewState) {
        layoutRoot.post { handleShowing(state) }
        layoutRoot.post { handlePresence(state) }
        layoutRoot.post { handleBottomMargin(state) }
        layoutRoot.post { handleError(state) }
        layoutRoot.post { handleUndo(state) }
    }

    private fun handlePresence(state: DetailViewState) {
        // Hide filter button for NEED
        if (state.listItemPresence == FridgeItem.Presence.NEED) {
            binding.detailFilterItem.isVisible = false
        }
    }

    private fun handleShowing(state: DetailViewState) {
        state.showing.let { showing ->
            disposeFilterLoaded()
            filterIconLoaded = imageLoader
                .load(
                    when (showing) {
                        DetailViewState.Showing.FRESH -> R.drawable.ic_open_in_browser_24dp
                        DetailViewState.Showing.CONSUMED -> R.drawable.ic_consumed_24dp
                        DetailViewState.Showing.SPOILED -> R.drawable.ic_spoiled_24dp
                    }
                )
                .into(binding.detailFilterItem)
        }
    }

    private fun handleBottomMargin(state: DetailViewState) {
        state.bottomOffset.let { height ->
            if (height > 0) {
                layoutRoot.updatePadding(bottom = height)
            }
        }
    }

    private fun handleError(state: DetailViewState) {
        state.listError.let { throwable ->
            if (throwable == null) {
                clearListError()
            } else {
                showListError(throwable)
            }
        }
    }

    private fun handleUndo(state: DetailViewState) {
        state.undoableItem.let { undoable ->
            if (undoable == null) {
                clearUndoSnackbar()
            } else {
                showUndoSnackbar(undoable)
            }
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

    private fun showUndoSnackbar(undoable: FridgeItem) {
        val message = when {
            undoable.isConsumed() -> "Consumed ${undoable.name()}"
            undoable.isSpoiled() -> "${undoable.name()} spoiled"
            else -> "Removed ${undoable.name()}"
        }
        Snackbreak.bindTo(owner, "undo") {
            short(layoutRoot, message,
                onHidden = { _, _ ->
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
}
