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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.ExpandCountBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class ExpandItemCount @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<ExpandItemViewState, ExpandedItemViewEvent, ExpandCountBinding>(parent) {

    override val viewBinding = ExpandCountBinding::inflate

    override val layoutRoot by boundView { expandItemCount }

    private var firstRender = true

    init {
        doOnTeardown {
            firstRender = false
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        binding.expandItemCountEditable.text.clear()
        binding.expandItemCountEditable.setOnDebouncedClickListener(null)
    }

    private fun setCount(item: FridgeItem) {
        val count = item.count()
        val countText = if (count > 0) "$count" else ""
        binding.expandItemCountEditable.setTextKeepState(countText)
    }

    override fun onRender(state: ExpandItemViewState) {
        handleItem(state)
    }

    private fun handleItem(state: ExpandItemViewState) {
        state.item.let { item ->
            if (item != null) {
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
        binding.expandItemCountEditable.addTextChangedListener(watcher)
        return watcher
    }

    private fun removeWatcher(watcher: TextWatcher) {
        binding.expandItemCountEditable.removeTextChangedListener(watcher)
    }

    private fun commit() {
        val count = binding.expandItemCountEditable.text.toString().toIntOrNull() ?: 0
        publish(ExpandedItemViewEvent.CommitCount(count))
    }
}
