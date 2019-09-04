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

package com.pyamsoft.fridge.detail.item

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitCount
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemCount @Inject internal constructor(
    parent: ViewGroup,
    private val initialItem: FridgeItem
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.detail_list_item_count

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_count)

    private val countView by boundView<EditText>(R.id.detail_item_count_editable)

    private var countWatcher: TextWatcher? = null

    // Don't bind nameView text based on state
    // Android does not re-render fast enough for edits to keep up
    override fun onInflated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        setCount(item = initialItem)
    }

    private fun setCount(item: FridgeItem) {
        val count = item.count()
        val countText = if (count > 0) "$count" else ""
        countView.setTextKeepState(countText)
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        state.item.let { item ->
            removeListeners()
            addWatcher(item)
        }

        val isEditable = state.isEditable
        val item = state.item

        if (isEditable) {
            removeListeners()
            addWatcher(item)
        } else {
            setCount(item)
            countView.setNotEditable()
            countView.setOnDebouncedClickListener {
                publish(ExpandItem(item))
            }
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
        countView.addTextChangedListener(watcher)
        countWatcher = watcher
    }

    override fun onTeardown() {
        removeListeners()
        countView.text.clear()
        countView.setOnDebouncedClickListener(null)
    }

    private fun removeListeners() {
        countWatcher?.let { countView.removeTextChangedListener(it) }
        countWatcher = null
    }

    private fun commit(item: FridgeItem) {
        val count = countView.text.toString().toIntOrNull() ?: 0
        publish(CommitCount(item, count))
    }
}
