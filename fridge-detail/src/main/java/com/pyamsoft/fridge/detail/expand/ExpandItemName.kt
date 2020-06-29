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
import android.text.InputType
import android.text.TextWatcher
import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.base.BaseItemName
import timber.log.Timber
import javax.inject.Inject

class ExpandItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseItemName<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private val popupWindow = SimilarlyNamedListWindow(parent.context)
    private var firstRender = true

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
            binding.detailItemNameEditable.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    popupWindow.show()
                } else {
                    popupWindow.dismiss()
                }
            }
        }

        doOnTeardown {
            binding.detailItemNameEditable.onFocusChangeListener = null
        }

        doOnTeardown {
            popupWindow.teardown()
        }

        doOnTeardown {
            firstRender = false
        }
    }

    private fun selectSimilar(item: FridgeItem) {
        Timber.d("Similar popup FridgeItem selected: $item")
        setName(item)
        publish(ExpandedItemViewEvent.SelectSimilar(item))
    }

    override fun onRender(state: ExpandItemViewState) {
        handlePopupWindow(state)
        handleItem(state)
    }

    private fun handleItem(state: ExpandItemViewState) {
        state.item.let { item ->
            if (item != null) {
                if (firstRender) {
                    firstRender = false
                    setName(item)
                    val watcher = addWatcher()
                    doOnTeardown {
                        removeListeners(watcher)
                    }
                }
            }
            val isEditable = if (item == null) false else !item.isArchived()
            binding.detailItemNameEditable.inputType =
                if (isEditable) EDITABLE_INPUT_TYPE else InputType.TYPE_NULL
            binding.detailItemNameEditable.isFocusable = isEditable
            binding.detailItemNameEditable.setTextIsSelectable(isEditable)
            binding.detailItemNameEditable.isLongClickable = isEditable
        }
    }

    private fun handlePopupWindow(state: ExpandItemViewState) {
        popupWindow.set(state.similarItems, binding.detailItemNameEditable.isFocused)
    }

    @CheckResult
    private fun addWatcher(): TextWatcher {
        val watcher = object : TextWatcher {

            override fun afterTextChanged(s: Editable?) {
                s?.also { editable -> commit(editable.toString()) }
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
        binding.detailItemNameEditable.addTextChangedListener(watcher)
        return watcher
    }

    private fun removeListeners(watcher: TextWatcher) {
        watcher.let { binding.detailItemNameEditable.removeTextChangedListener(it) }
    }

    private fun commit(name: String) {
        publish(ExpandedItemViewEvent.CommitName(name))
    }

    companion object {
        private const val EDITABLE_INPUT_TYPE =
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
}
