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

import androidx.core.view.updatePadding
import com.pyamsoft.pydroid.ui.databinding.ListitemFrameBinding
import com.pyamsoft.pydroid.util.toDp

internal class SpacerItemViewHolder internal constructor(
    binding: ListitemFrameBinding,
    space: Int
) : DetailViewHolder<ListitemFrameBinding>(binding) {

    init {
        val offset = space.toDp(binding.listitemFrame.context)
        binding.listitemFrame.updatePadding(top = offset)
    }

    override fun bind(state: DetailListItemViewState) {
    }
}
