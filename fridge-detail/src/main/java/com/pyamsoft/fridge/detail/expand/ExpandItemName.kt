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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.base.BaseItemName
import com.pyamsoft.fridge.detail.isEditable
import com.pyamsoft.fridge.detail.setEditable
import javax.inject.Inject
import timber.log.Timber

class ExpandItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseItemName<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    private val popupWindow = SimilarlyNamedListWindow(parent.context)

    // NOTE(Peter): Hack because Android does not allow us to use Controlled view components like
    // React does by binding input and drawing to the render loop.
    //
    // This initialRenderPerformed variable allows us to set the initial state of a view once, and bind listeners to
    // it because the state.item is only available in render instead of inflate. Once the firstRender
    // has set the view component up, the actual input will no longer be tracked via state render events,
    // so the input is uncontrolled.
    private var initialRenderPerformed = false

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
    }

    private fun selectSimilar(item: FridgeItem) {
        Timber.d("Similar popup FridgeItem selected: $item")
        setName(item)
        publish(ExpandedItemViewEvent.SelectSimilar(item))
    }

    override fun onRender(state: ExpandItemViewState) {
        handlePopupWindow(state)
        handleInitialRender(state)
        handleItem(state)
    }

    private fun handleInitialRender(state: ExpandItemViewState) {
        if (initialRenderPerformed) {
            return
        }
        state.item?.let { item ->
            initialRenderPerformed = true
            setName(item)
            watchUntilTeardown(item.name())
        }
    }

    private fun handleItem(state: ExpandItemViewState) {
        state.item.let { item ->
            val isEditable = if (item == null) false else !item.isArchived()
            if (binding.detailItemNameEditable.isEditable != isEditable) {
                val inputType = if (isEditable) EDITABLE_INPUT_TYPE else InputType.TYPE_NULL
                binding.detailItemNameEditable.setEditable(inputType)
            }
        }
    }

    private fun handlePopupWindow(state: ExpandItemViewState) {
        popupWindow.set(state.similarItems, binding.detailItemNameEditable.isFocused)
    }

    private fun watchUntilTeardown(previousText: String) {
        val watcher = object : TextWatcher {

            private var oldText = previousText

            override fun afterTextChanged(s: Editable) {
                val newText = s.toString()
                if (newText != oldText) {
                    oldText = newText
                    publish(ExpandedItemViewEvent.CommitName(newText))
                }
            }

            override fun beforeTextChanged(
                s: CharSequence,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
            }
        }
        binding.detailItemNameEditable.addTextChangedListener(watcher)
        doOnTeardown {
            binding.detailItemNameEditable.removeTextChangedListener(watcher)
        }
    }

    companion object {
        private const val EDITABLE_INPUT_TYPE =
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
}
