/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge.detail.expand

import android.text.InputType
import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.ui.isEditable
import com.pyamsoft.fridge.ui.setEditable
import com.pyamsoft.fridge.ui.view.UiEditText
import timber.log.Timber
import javax.inject.Inject

class ExpandItemName @Inject internal constructor(
    parent: ViewGroup
) : UiEditText<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private val popupWindow = SimilarlyNamedListWindow(parent.context)

    init {
        doOnInflate {
            popupWindow.apply {
                initializeView(layoutRoot)
                setOnDismissListener {
                    Timber.d("Similar popup dismissed")
                }
                setOnItemClickListener { selectSimilar(it) }
            }
        }

        doOnInflate {
            binding.uiEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    popupWindow.show()
                } else {
                    popupWindow.dismiss()
                }
            }
        }

        doOnTeardown {
            binding.uiEditText.onFocusChangeListener = null
        }

        doOnTeardown {
            popupWindow.teardown()
        }
    }

    override fun onTextChanged(oldText: String, newText: String) {
        publish(ExpandedItemViewEvent.CommitName(newText))
    }

    private fun selectSimilar(item: FridgeItem) {
        Timber.d("Similar popup FridgeItem selected: $item")
        setText(item.name())
        publish(ExpandedItemViewEvent.SelectSimilar(item))
    }

    override fun onRender(state: ExpandItemViewState) {
        super.onRender(state)
        handlePopupWindow(state)
        handleItem(state)
    }

    private fun handleItem(state: ExpandItemViewState) {
        state.item.let { item ->
            val isEditable = if (item == null) false else !item.isArchived()
            if (binding.uiEditText.isEditable != isEditable) {
                val inputType = if (isEditable) EDITABLE_INPUT_TYPE else InputType.TYPE_NULL
                binding.uiEditText.setEditable(inputType)
            }
        }
    }

    private fun handlePopupWindow(state: ExpandItemViewState) {
        popupWindow.set(state.similarItems, binding.uiEditText.isFocused)
    }

    companion object {
        private const val EDITABLE_INPUT_TYPE =
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
}
