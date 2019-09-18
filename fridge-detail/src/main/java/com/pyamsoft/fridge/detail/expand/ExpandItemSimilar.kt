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

import android.os.Bundle
import android.view.View
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

    override fun onInflated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        message.isVisible = false
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        state.sameNamedItems.let { similar ->
            if (similar.isEmpty()) {
                message.isVisible = false
                message.text = ""
            } else {
                val item = state.item
                message.isVisible = true
                message.text =
                    "'${item.name().trim()}' is similar to other items you already own, are you sure you need another?"
            }
        }
    }

    override fun onTeardown() {
        message.isVisible = false
        message.text = ""
    }
}
