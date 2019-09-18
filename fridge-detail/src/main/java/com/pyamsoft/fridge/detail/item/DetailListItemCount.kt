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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.base.BaseItemCount
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject
import javax.inject.Named

class DetailListItemCount @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    parent: ViewGroup,
    initialItem: FridgeItem
) : BaseItemCount(parent, initialItem) {

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        if (!isEditable) {
            return
        }

        val item = state.item
        setCount(item)
        countView.setNotEditable()
        countView.setOnDebouncedClickListener {
            publish(ExpandItem(item))
        }
    }
}