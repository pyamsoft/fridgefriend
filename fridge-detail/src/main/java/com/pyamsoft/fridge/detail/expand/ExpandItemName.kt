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
import com.pyamsoft.fridge.detail.base.BaseItemName
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent.CommitName
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.UiSavedState
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class ExpandItemName @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    parent: ViewGroup,
    initialItem: FridgeItem
) : BaseItemName(parent, initialItem) {

    private var nameWatcher: TextWatcher? = null
    private val popupWindow = SimilarlyNamedListWindow(parent.context)

    init {
        popupWindow.apply {
            initializeView(nameView)
            setOnDismissListener {
                Timber.d("Popup dismissed")
            }
            setOnItemClickListener { item ->
                Timber.d("FridgeItem selected: $item")
            }
        }
    }

    override fun onRender(
        state: DetailItemViewState,
        savedState: UiSavedState
    ) {
        if (!isEditable) {
            return
        }

        val item = state.item
        removeListeners()
        addWatcher(item)

        popupWindow.set(state.similarItems)
    }

    private fun addWatcher(item: FridgeItem) {
        val watcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                s?.also { editable ->
                    commit(item, editable.toString())
                }
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
        nameView.addTextChangedListener(watcher)
        nameWatcher = watcher
    }

    override fun onAfterTeardown() {
        removeListeners()
        popupWindow.teardown()
    }

    private fun removeListeners() {
        nameWatcher?.let { nameView.removeTextChangedListener(it) }
        nameWatcher = null
    }

    private fun commit(item: FridgeItem, name: String) {
        publish(CommitName(item, name))
    }
}
