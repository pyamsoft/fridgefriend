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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.base.BaseItemCount
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject
import javax.inject.Named

class ExpandItemCount @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    parent: ViewGroup,
    initialItem: FridgeItem
) : BaseItemCount<ExpandItemViewState, ExpandedItemViewEvent>(parent, initialItem) {

    private var countWatcher: TextWatcher? = null

    init {
        doOnTeardown {
            removeListeners()
        }
    }

    override fun onRender(
        state: ExpandItemViewState,
        savedState: UiSavedState
    ) {
        if (!isEditable) {
            return
        }

        removeListeners()
        state.item.let { item ->
            if (item == null) {
                clear()
            } else {
                addWatcher(item)
            }
        }
    }

    private fun addWatcher(item: FridgeItem) {
        val watcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                commit(item)
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
        countWatcher = watcher
    }

    private fun removeListeners() {
        countWatcher?.let { countView.removeTextChangedListener(it) }
        countWatcher = null
    }

    private fun commit(item: FridgeItem) {
        val count = countView.text.toString().toIntOrNull() ?: 0
        publish(ExpandedItemViewEvent.CommitCount(item, count))
    }
}
