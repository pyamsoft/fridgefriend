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

package com.pyamsoft.fridge.search

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.search.databinding.SearchContainerBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

class SearchContainer
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ListEvent, SearchContainerBinding>(parent) {

  override val viewBinding = SearchContainerBinding::inflate

  override val layoutRoot by boundView { searchContainer }

  fun layout(func: ConstraintSet.() -> Unit) {
    return binding.searchContainer.layout(func)
  }

  override fun onRender(state: UiRender<DetailViewState>) {
    state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
  }

  private fun handleBottomMargin(height: Int) {
    layoutRoot.updatePadding(bottom = height)
  }
}
