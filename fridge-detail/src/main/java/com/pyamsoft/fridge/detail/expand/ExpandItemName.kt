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
import com.pyamsoft.pydroid.arch.UiSavedState
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class ExpandItemName @Inject internal constructor(
    @Named("item_editable") private val isEditable: Boolean,
    parent: ViewGroup,
    initialItem: FridgeItem
) : BaseItemName<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private var nameWatcher: TextWatcher? = null
    private val popupWindow = SimilarlyNamedListWindow(parent.context)

    init {
        doOnInflate {
            setName(initialItem, null)
        }

        doOnInflate {
            popupWindow.apply {
                initializeView(layoutRoot)
                setOnDismissListener {
                    Timber.d("Similar popup dismissed")
                }
                setOnItemClickListener { selectedItem ->
                    Timber.d("Similar popup FridgeItem selected: $selectedItem")
                    // TODO publish SELECT_SIMILAR event to VM
                    setName(selectedItem, null)
                }
            }
        }

        doOnTeardown {
            removeListeners()
            popupWindow.teardown()
        }
    }

    override fun onRender(
        state: ExpandItemViewState,
        savedState: UiSavedState
    ) {
        if (!isEditable) {
            return
        }

        state.item.let { item ->
            removeListeners()
            if (item == null) {
                clear()
            } else {
                addWatcher(item)
                popupWindow.set(if (nameView.isFocused) state.similarItems else emptyList())
            }
        }
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

    private fun removeListeners() {
        nameWatcher?.let { nameView.removeTextChangedListener(it) }
        nameWatcher = null
    }

    private fun commit(item: FridgeItem, name: String) {
        publish(ExpandedItemViewEvent.CommitName(item, name))
    }
}
