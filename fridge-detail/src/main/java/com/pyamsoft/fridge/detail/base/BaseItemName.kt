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

package com.pyamsoft.fridge.detail.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

abstract class BaseItemName protected constructor(
    parent: ViewGroup,
    private val initialItem: FridgeItem
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    final override val layout: Int = R.layout.detail_list_item_name

    final override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_name)

    protected val nameView by boundView<EditText>(R.id.detail_item_name_editable)

    // Don't bind nameView text based on state
    // Android does not re-render fast enough for edits to keep up
    final override fun onInflated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        setName(initialItem)
        onAfterInflated(view, savedInstanceState)
    }

    protected fun setName(item: FridgeItem) {
        nameView.setTextKeepState(item.name())
    }

    final override fun onTeardown() {
        nameView.text.clear()
        nameView.setOnDebouncedClickListener(null)
        onAfterTeardown()
    }

    protected open fun onAfterInflated(view: View, savedInstanceState: Bundle?) {
    }

    protected open fun onAfterTeardown() {
    }
}
