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

package com.pyamsoft.fridge.detail.expand

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

class ExpandItemName @Inject internal constructor(
  parent: ViewGroup,
  private val initialItem: FridgeItem
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

  override val layout: Int = R.layout.detail_list_item_name

  override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_name)

  private val nameView by boundView<EditText>(R.id.detail_item_name_editable)

  private var nameWatcher: TextWatcher? = null

  // Don't bind nameView text based on state
  // Android does not re-render fast enough for edits to keep up
  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    nameView.setTextKeepState(initialItem.name())
  }

  override fun onRender(
    state: DetailItemViewState,
    savedState: UiSavedState
  ) {
    state.item.let { item ->
      removeListeners()
      addWatcher(item)
    }
  }

  private fun addWatcher(item: FridgeItem) {
    val watcher = object : TextWatcher {

      override fun afterTextChanged(s: Editable?) {
        commit(item)
      }

      override fun beforeTextChanged(
        s: CharSequence?,
        start: Int,
        count: Int,
        after: Int
      ) {
      }

      override fun onTextChanged(
        s: CharSequence?,
        start: Int,
        before: Int,
        count: Int
      ) {
      }

    }
    nameView.addTextChangedListener(watcher)
    nameWatcher = watcher
  }

  override fun onTeardown() {
    removeListeners()
    nameView.text.clear()
  }

  private fun removeListeners() {
    nameWatcher?.let { nameView.removeTextChangedListener(it) }
    nameWatcher = null
  }

  private fun commit(item: FridgeItem) {
    val name = nameView.text.toString()
    publish(CommitName(item, name))
  }
}

