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

package com.pyamsoft.fridge.entry

import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.databinding.EntryAddNewBinding
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.disposeOnDestroy
import com.pyamsoft.pydroid.ui.util.Snackbreak
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class EntryAddNew
@Inject
internal constructor(
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
) :
    BaseUiView<EntryViewState, EntryViewEvent.AddEvent, EntryAddNewBinding>(parent),
    SnackbarContainer {

  override val viewBinding = EntryAddNewBinding::inflate

  override val layoutRoot by boundView { entryAddNewRoot }

  init {
    doOnInflate {
      imageLoader
          .asDrawable()
          .load(R.drawable.ic_add_24dp)
          .into(binding.entryAddNew)
          .disposeOnDestroy(owner)
    }

    doOnInflate {
      binding.entryAddNew.setOnDebouncedClickListener { publish(EntryViewEvent.AddEvent.AddNew) }
    }

    doOnTeardown { binding.entryAddNew.setOnClickListener(null) }

    doOnInflate { binding.entryAddNew.show() }
  }

  override fun container(): CoordinatorLayout {
    return layoutRoot
  }

  override fun onRender(state: UiRender<EntryViewState>) {
    state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
    state.mapChanged { it.undoableEntry }.render(viewScope) { handleUndo(it) }
  }

  private fun handleBottomMargin(height: Int) {
    // Multiply by 2 to account for the bar offset and the height change in MainContainer
    layoutRoot.updateLayoutParams<ViewGroup.MarginLayoutParams> { this.bottomMargin = height * 2 }
  }

  private fun handleUndo(undoable: FridgeEntry?) {
    if (undoable != null) {
      showUndoSnackbar(undoable)
    }
  }

  private fun showUndoSnackbar(undoable: FridgeEntry) {
    Snackbreak.bindTo(owner) {
      long(
          layoutRoot,
          "Removed ${undoable.name()}",
          onHidden = { _, _ -> publish(EntryViewEvent.AddEvent.ReallyDeleteEntryNoUndo) }) {
        // Restore the old item
        setAction("Undo") { publish(EntryViewEvent.AddEvent.UndoDeleteEntry) }
      }
    }
  }
}
