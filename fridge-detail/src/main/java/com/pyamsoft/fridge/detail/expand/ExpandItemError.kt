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
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class ExpandItemError @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    override val layout: Int = R.layout.expand_error

    override val layoutRoot by boundView<ViewGroup>(R.id.expand_item_error_root)
    private val message by boundView<TextView>(R.id.expand_item_error_msg)

    init {
        doOnInflate {
            // No errors initially right
            message.isVisible = false
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        message.isVisible = false
        message.text = ""
    }

    override fun onRender(state: ExpandItemViewState) {
        state.throwable.let { throwable ->
            if (throwable == null) {
                clear()
            } else {
                message.isVisible = true
                message.text = throwable.message ?: "An unknown error occurred"
            }
        }
    }
}
