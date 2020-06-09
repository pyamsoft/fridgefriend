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
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.SnackbarContainer
import javax.inject.Inject

class DetailSnackbarContainer @Inject internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup
) : SnackbarContainer<DetailViewState, DetailViewEvent>(owner, parent) {

    override fun onRender(state: DetailViewState) {
        layoutRoot.post { handleBottomMargin(state) }
        layoutRoot.post { handleError(state) }
        layoutRoot.post { handleUndo(state) }
    }

    private fun handleBottomMargin(state: DetailViewState) {
        state.bottomOffset.let { height ->
            if (height > 0) {
                addBottomPadding(height)
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
        makeSnackbar("list", throwable.message ?: "An unexpected error has occurred.")
    }

    private fun clearListError() {
        dismissSnackbar("list")
    }

    private fun showUndoSnackbar(undoable: FridgeItem) {
        withSnackbar("undo") {
            val message = when {
                undoable.isConsumed() -> "Consumed ${undoable.name()}"
                undoable.isSpoiled() -> "${undoable.name()} spoiled"
                else -> "Removed ${undoable.name()}"
            }
            short(layoutRoot, message, onHidden = { _, _ ->
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
        dismissSnackbar("undo")
    }
}