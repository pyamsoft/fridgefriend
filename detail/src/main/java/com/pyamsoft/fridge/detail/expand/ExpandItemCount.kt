/*
 * Copyright 2021 Peter Kenji Yamanaka
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
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.databinding.ExpandCountBinding
import com.pyamsoft.fridge.ui.isEditable
import com.pyamsoft.fridge.ui.setEditable
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class ExpandItemCount
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<ExpandedViewState, ExpandedViewEvent.ItemEvent, ExpandCountBinding>(parent) {

  override val viewBinding = ExpandCountBinding::inflate

  override val layoutRoot by boundView { expandItemCount }

  private var delegate: UiEditTextDelegate? = null

  init {
    doOnInflate {
      delegate =
          UiEditTextDelegate.create(binding.expandItemCountEditable) { newText ->
            publish(ExpandedViewEvent.ItemEvent.CommitCount(newText.toIntOrNull() ?: 0))
            return@create true
          }
              .apply { handleCreate() }
    }

    doOnTeardown {
      delegate?.handleTeardown()
      delegate = null
    }
  }

  override fun onRender(state: UiRender<ExpandedViewState>) {
    state.mapChanged { it.item }.render(viewScope) { handleEditable(it) }
    state.mapChanged { it.item }.render(viewScope) { handleCount(it) }
  }

  @CheckResult
  private fun getCountAsText(item: FridgeItem): String {
    val count = item.count()
    return if (count > 0) "$count" else ""
  }

  private fun handleCount(item: FridgeItem?) {
    item?.let { requireNotNull(delegate).handleTextChanged(getCountAsText(it)) }
  }

  private fun handleEditable(item: FridgeItem?) {
    val isEditable = if (item == null) false else !item.isArchived()
    if (binding.expandItemCountEditable.isEditable != isEditable) {
      val inputType = if (isEditable) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_NULL
      binding.expandItemCountEditable.setEditable(inputType)
    }
  }
}
