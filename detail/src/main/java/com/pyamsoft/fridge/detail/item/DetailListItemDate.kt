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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.base.BaseItemDate
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class DetailListItemDate
@Inject
internal constructor(
    imageLoader: ImageLoader,
    theming: ThemeProvider,
    parent: ViewGroup,
) : BaseItemDate<DetailItemViewState, DetailItemViewEvent>(imageLoader, theming, parent) {

  init {
    doOnInflate {
      layoutRoot.setOnDebouncedClickListener { publish(DetailItemViewEvent.ExpandItem) }
    }

    doOnTeardown { layoutRoot.setOnDebouncedClickListener(null) }
  }

  private fun handleItem(item: FridgeItem) {
    require(item.isReal()) { "Cannot render non-real item: $item" }
    renderItem(item)
  }

  private fun handlePresence(item: FridgeItem) {
    val presence = item.presence()
    if (presence == FridgeItem.Presence.NEED) {
      layoutRoot.isVisible = true
    } else {
      layoutRoot.isGone = true
    }
  }

  override fun onRender(state: UiRender<DetailItemViewState>) {
    state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
    state.mapChanged { it.item }.render(viewScope) { handlePresence(it) }
  }
}
