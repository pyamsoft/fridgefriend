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
import com.pyamsoft.fridge.detail.databinding.ExpandPurchasedBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import java.text.DateFormat
import javax.inject.Inject

class ExpandItemPurchasedDate
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<ExpandedViewState, ExpandedViewEvent, ExpandPurchasedBinding>(parent) {

  override val viewBinding = ExpandPurchasedBinding::inflate

  override val layoutRoot by boundView { expandItemPurchased }

  init {
    doOnInflate { layoutRoot.isVisible = false }

    doOnTeardown {
      binding.expandItemPurchasedOn.text = null
      layoutRoot.isVisible = false
    }
  }

  override fun onRender(state: UiRender<ExpandedViewState>) {
    state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
  }

  private fun handleItem(item: FridgeItem?) {
    if (item == null) {
      layoutRoot.isVisible = false
      return
    }

    if (item.presence() != FridgeItem.Presence.HAVE) {
      layoutRoot.isVisible = false
      return
    }

    val purchasedOn = item.purchaseTime()
    if (purchasedOn == null) {
      layoutRoot.isVisible = false
      return
    }

    val formatted = dateFormatter.format(purchasedOn)
    layoutRoot.isVisible = true
    binding.expandItemPurchasedOn.text = formatted
  }

  companion object {

    // Don't need to ThreadLocal since it will always be accessed by UI
    private val dateFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM)
  }
}
