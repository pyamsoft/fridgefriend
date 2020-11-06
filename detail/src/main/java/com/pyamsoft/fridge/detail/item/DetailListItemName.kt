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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import com.pyamsoft.fridge.ui.setNotEditable
import com.pyamsoft.fridge.ui.view.UiEditText
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemName @Inject internal constructor(
    parent: ViewGroup
) : UiEditText<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val isWatchingForTextChanges = false

    init {
        doOnInflate {
            binding.uiEditText.setNotEditable()
            binding.uiEditText.setOnDebouncedClickListener {
                publish(DetailItemViewEvent.ExpandItem)
            }
        }
    }

    private fun handleItem(state: DetailItemViewState) {
        state.item.let { item ->
            require(item.isReal()) { "Cannot render non-real item: $item" }
            setText(item.name())
        }
    }

    override fun onRender(state: DetailItemViewState) {
        super.onRender(state)
        handleItem(state)
    }
}
