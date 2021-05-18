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

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.databinding.ExpandSimilarBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class ExpandItemSimilar
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<ExpandedViewState, ExpandedViewEvent, ExpandSimilarBinding>(parent) {

  override val viewBinding = ExpandSimilarBinding::inflate

  override val layoutRoot by boundView { expandItemSimilar }

  init {
    doOnInflate {
      // No similar by default
      binding.expandItemSimilarMsg.isVisible = false
    }

    doOnTeardown { clear() }
  }

  override fun onRender(state: UiRender<ExpandedViewState>) {
    state.render(viewScope) { handleSameNamedItems(it) }
  }

  private fun clear() {
    binding.expandItemSimilarMsg.isVisible = false
    binding.expandItemSimilarMsg.text = null
  }

  private fun handleSameNamedItems(state: ExpandedViewState) {
    val sameName = state.sameNamedItems
    val item = state.item
    if (sameName.isEmpty() || item == null || item.presence() != FridgeItem.Presence.NEED) {
      clear()
    } else {
      val name = item.name().trim()
      val count = sameName.filterNot { it.isArchived() }.count()
      val message = "You already have at least $count '$name', do you need more?"
      binding.expandItemSimilarMsg.isVisible = true
      binding.expandItemSimilarMsg.text = message
    }
  }
}
