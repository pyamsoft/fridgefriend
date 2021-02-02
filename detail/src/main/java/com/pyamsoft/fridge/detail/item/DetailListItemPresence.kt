/*
 * Copyright 2021 Peter Kenji Yamanaka
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
import com.pyamsoft.fridge.detail.base.BaseItemPresence
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitPresence
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class DetailListItemPresence @Inject internal constructor(
    parent: ViewGroup,
) : BaseItemPresence<DetailItemViewState, DetailItemViewEvent>(parent) {

    override fun onRender(state: UiRender<DetailItemViewState>) {
        state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
    }

    private fun handleItem(item: FridgeItem) {
        require(item.isReal()) { "Cannot render non-real item: $item" }
        renderItem(item)
    }

    override fun publishChangePresence() {
        publish(CommitPresence)
    }
}
