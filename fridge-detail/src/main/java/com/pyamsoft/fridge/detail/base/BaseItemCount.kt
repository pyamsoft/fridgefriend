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

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailListItemCountBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

abstract class BaseItemCount<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BindingUiView<S, V, DetailListItemCountBinding>(parent) {

    final override val viewBinding by viewBinding(DetailListItemCountBinding::inflate)

    final override val layoutRoot by boundView { detailItemCount }

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        binding.detailItemCountEditable.text.clear()
        binding.detailItemCountEditable.setOnDebouncedClickListener(null)
    }

    protected fun setCount(item: FridgeItem) {
        val count = item.count()
        val countText = if (count > 0) "$count" else ""
        binding.detailItemCountEditable.setTextKeepState(countText)
    }
}
