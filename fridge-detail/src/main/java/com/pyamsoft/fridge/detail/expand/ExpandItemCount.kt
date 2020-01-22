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

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.base.BaseItemCount
import javax.inject.Inject

class ExpandItemCount @Inject internal constructor(
    parent: ViewGroup
) : BaseItemCount<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private var firstRender = true

    init {
        doOnTeardown {
            firstRender = false
        }
    }

    override fun onRender(state: ExpandItemViewState) {
        state.item.let { item ->
            if (item == null) {
                countView.isVisible = false
            } else {
                if (firstRender) {
                    firstRender = false
                    setCount(item)
                    val watcher = createWatcher()
                    doOnTeardown {
                        removeWatcher(watcher)
                    }
                }
            }
        }
    }

    @CheckResult
    private fun createWatcher(): TextWatcher {
        val watcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                commit()
            }

            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
        countView.addTextChangedListener(watcher)
        return watcher
    }

    private fun removeWatcher(watcher: TextWatcher) {
        countView.removeTextChangedListener(watcher)
    }

    private fun commit() {
        val count = countView.text.toString().toIntOrNull() ?: 0
        publish(ExpandedItemViewEvent.CommitCount(count))
    }
}
