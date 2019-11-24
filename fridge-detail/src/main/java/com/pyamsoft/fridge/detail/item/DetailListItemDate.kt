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
import com.pyamsoft.fridge.detail.base.BaseItemDate
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.ExpandItem
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject
import javax.inject.Named

class DetailListItemDate @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseItemDate(imageLoader, parent) {

    override fun afterRender(
        month: Int,
        day: Int,
        year: Int,
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        if (isEditable) {
            return
        }

        val item = state.item
        layoutRoot.setOnDebouncedClickListener { publish(ExpandItem(item)) }
    }
}
