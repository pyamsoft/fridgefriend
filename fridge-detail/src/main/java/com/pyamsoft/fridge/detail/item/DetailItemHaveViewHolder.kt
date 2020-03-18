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

import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import javax.inject.Inject

class DetailItemHaveViewHolder internal constructor(
    binding: DetailListItemHolderBinding,
    owner: LifecycleOwner,
    editable: Boolean,
    callback: DetailListAdapter.Callback,
    factory: DetailItemComponent.Factory
) : DetailItemViewHolder(binding, owner, callback) {

    @JvmField
    @Inject
    internal var glancesView: DetailListItemGlances? = null

    @JvmField
    @Inject
    internal var countView: DetailListItemCount? = null

    init {
        factory.create(binding.detailListItem, editable).inject(this)

        val count = requireNotNull(countView)
        val glances = requireNotNull(glancesView)
        create(binding.detailListItem, count, glances)
    }
}
