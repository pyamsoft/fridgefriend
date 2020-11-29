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
import com.pyamsoft.fridge.detail.databinding.ExpandNameBinding
import com.pyamsoft.fridge.ui.isEditable
import com.pyamsoft.fridge.ui.setEditable
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.pydroid.arch.BaseUiView
import timber.log.Timber
import javax.inject.Inject

class ExpandItemName @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<ExpandItemViewState, ExpandedItemViewEvent, ExpandNameBinding>(parent) {

    override val viewBinding = ExpandNameBinding::inflate

    override val layoutRoot by boundView { expandItemNameContainer }

    private val popupWindow = SimilarlyNamedListWindow(parent.context)

    private val delegate by lazy {
        UiEditTextDelegate(binding.expandItemName) { _, newText ->
            publish(ExpandedItemViewEvent.CommitName(newText))
        }
    }

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
            binding.expandItemName.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    popupWindow.show()
                } else {
                    popupWindow.dismiss()
                }
            }
        }

        doOnTeardown {
            binding.expandItemName.onFocusChangeListener = null
        }

        doOnTeardown {
            popupWindow.teardown()
        }

        doOnInflate {
            delegate.create()
        }

        doOnTeardown {
            delegate.destroy()
        }
    }

    private fun selectSimilar(item: FridgeItem) {
        Timber.d("Similar popup FridgeItem selected: $item")
        delegate.setText(item.name())
        publish(ExpandedItemViewEvent.SelectSimilar(item))
    }

    override fun onRender(state: ExpandItemViewState) {
        handleName(state)
        handlePopupWindow(state)
        handleEditable(state)
    }

    private fun handleName(state: ExpandItemViewState) {
        state.item?.let { item ->
            delegate.render(item.name())
        }
    }

    private fun handleEditable(state: ExpandItemViewState) {
        state.item.let { item ->
            val isEditable = if (item == null) false else !item.isArchived()
            if (binding.expandItemName.isEditable != isEditable) {
                val inputType = if (isEditable) EDITABLE_INPUT_TYPE else InputType.TYPE_NULL
                binding.expandItemName.setEditable(inputType)
            }
        }
    }

    private fun handlePopupWindow(state: ExpandItemViewState) {
        popupWindow.set(state.similarItems, binding.expandItemName.isFocused)
    }

    companion object {
        private const val EDITABLE_INPUT_TYPE =
            InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
    }
}
