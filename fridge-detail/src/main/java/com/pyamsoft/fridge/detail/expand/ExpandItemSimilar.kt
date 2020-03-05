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

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.databinding.ExpandSimilarBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import javax.inject.Inject

class ExpandItemSimilar @Inject internal constructor(
    parent: ViewGroup
) : BindingUiView<ExpandItemViewState, ExpandedItemViewEvent, ExpandSimilarBinding>(parent) {

    override val viewBinding by viewBinding(ExpandSimilarBinding::inflate)

    override val layoutRoot by boundView { expandItemSimilar }

    init {
        doOnInflate {
            // No similar by default
            binding.expandItemSimilarMsg.isVisible = false
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        binding.expandItemSimilarMsg.isVisible = false
        binding.expandItemSimilarMsg.text = null
    }

    override fun onRender(state: ExpandItemViewState) {
        state.sameNamedItems.let { similar ->
            if (similar.isEmpty()) {
                clear()
            } else {
                state.item.let { item ->
                    if (item != null) {
                        val name = item.name().trim()
                        binding.expandItemSimilarMsg.isVisible = true
                        binding.expandItemSimilarMsg.text =
                            "You already have at least ${similar.size} '$name', do you need another?"
                    }
                }
            }
        }
    }
}
