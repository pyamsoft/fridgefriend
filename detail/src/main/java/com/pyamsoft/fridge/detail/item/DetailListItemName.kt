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

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.base.BaseItemName
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseItemName<DetailListItemViewState, DetailItemViewEvent>(parent) {

    init {
        doOnInflate {
            binding.detailItemNameEditable.setNotEditable()
            binding.detailItemNameEditable.setOnDebouncedClickListener { publish(DetailItemViewEvent.ExpandItem) }
        }
    }

    private fun handleItem(state: DetailListItemViewState) {
        state.item.let { item ->
            require(item.isReal()) { "Cannot render non-real item: $item" }
            setName(item)
        }
    }

    override fun onRender(state: DetailListItemViewState) {
        handleItem(state)
    }
}
