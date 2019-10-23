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
import android.widget.TextView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

class ExpandItemSimilar @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    override val layout: Int = R.layout.expand_similar

    override val layoutRoot by boundView<ViewGroup>(R.id.expand_item_similar)
    private val message by boundView<TextView>(R.id.expand_item_similar_msg)

    init {
        doOnInflate {
            // No similar by default
            message.isVisible = false
        }

        doOnTeardown {
            message.isVisible = false
            message.text = ""
        }
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        val item = state.item
        state.sameNamedItems.let { similar ->
            if (similar.isEmpty()) {
                message.isVisible = false
                message.text = ""
            } else {
                val name = item.name().trim()
                message.isVisible = true
                message.text = "You already have at least one '$name', do you need another?"
            }
        }
    }
}
