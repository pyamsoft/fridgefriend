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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.DetailListItemNameBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class DetailListItemName @Inject internal constructor(
    parent: ViewGroup,
) : BaseUiView<DetailItemViewState, DetailItemViewEvent, DetailListItemNameBinding>(parent) {

    override val viewBinding = DetailListItemNameBinding::inflate

    override val layoutRoot by boundView { detailItemNameRoot }

    init {
        doOnTeardown {
            binding.detailItemName.text = ""
        }
    }

    override fun onRender(state: UiRender<DetailItemViewState>) {
        state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
    }

    private fun handleItem(item: FridgeItem) {
        binding.detailItemName.text = item.name()
    }

}
