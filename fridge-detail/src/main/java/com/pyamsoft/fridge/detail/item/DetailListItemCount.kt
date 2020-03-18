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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.databinding.DetailListItemCountBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemCount @Inject internal constructor(
    parent: ViewGroup
) : BindingUiView<DetailListItemViewState, DetailItemViewEvent, DetailListItemCountBinding>(parent) {

    override val viewBinding = DetailListItemCountBinding::inflate

    override val layoutRoot by boundView { detailItemCount }

    init {
        doOnInflate {
            binding.detailItemCountUp.setOnDebouncedClickListener {
                publish(DetailItemViewEvent.IncreaseCount)
            }

            binding.detailItemCountDown.setOnDebouncedClickListener {
                publish(DetailItemViewEvent.DecreaseCount)
            }
        }

        doOnTeardown {
            binding.detailItemCountUp.setOnDebouncedClickListener(null)
            binding.detailItemCountDown.setOnDebouncedClickListener(null)
        }
    }

    override fun onRender(state: DetailListItemViewState) {
        state.item.let { item ->
            binding.detailItemCountText.text = "${item.count()}"
        }
    }
}
