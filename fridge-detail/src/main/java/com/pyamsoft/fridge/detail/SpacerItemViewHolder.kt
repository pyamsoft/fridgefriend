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

package com.pyamsoft.fridge.detail

import android.view.View
import android.view.ViewGroup
import androidx.core.content.withStyledAttributes
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailListItemViewState
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.toDp

internal class SpacerItemViewHolder internal constructor(
    itemView: View
) : DetailViewHolder(itemView) {


    init {
        val frame = itemView.findViewById<ViewGroup>(R.id.listitem_frame)
        frame.doOnApplyWindowInsets { v, insets, padding ->
            val offset = 8.toDp(v.context)
            val toolbarTopMargin = padding.top + insets.systemWindowInsetTop + offset
            v.context.withStyledAttributes(R.attr.toolbarStyle, intArrayOf(R.attr.actionBarSize)) {
                val sizeId = getResourceId(0, 0)
                if (sizeId != 0) {
                    val toolbarHeight = v.context.resources.getDimensionPixelSize(sizeId)
                    v.updatePadding(top = toolbarTopMargin + toolbarHeight + (offset * 2))
                }
            }
        }
    }

    override fun bind(state: DetailListItemViewState) {
    }
}
