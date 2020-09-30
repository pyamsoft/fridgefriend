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
import com.pyamsoft.fridge.detail.databinding.DetailListItemContainerBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class DetailListItemContainer @Inject internal constructor(
    glances: DetailListItemGlances,
    date: DetailListItemDate,
    parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent, DetailListItemContainerBinding>(parent) {

    override val viewBinding = DetailListItemContainerBinding::inflate

    override val layoutRoot by boundView { detailItemContainer }

    init {
        nest(glances)
        nest(date)
    }

    override fun onRender(state: DetailItemViewState) {
    }

}
