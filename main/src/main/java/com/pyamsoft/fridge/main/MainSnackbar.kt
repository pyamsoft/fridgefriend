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

package com.pyamsoft.fridge.main

import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import com.pyamsoft.fridge.main.databinding.MainSnackbarBinding
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class MainSnackbar
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<MainViewState, MainViewEvent, MainSnackbarBinding>(parent), SnackbarContainer {

  override val layoutRoot by boundView { mainSnackbar }

  override val viewBinding = MainSnackbarBinding::inflate

  override fun container(): CoordinatorLayout {
    return layoutRoot
  }

  override fun onRender(state: UiRender<MainViewState>) {
    state.mapChanged { it.bottomBarHeight }.render(viewScope) { handleBottomBarHeight(it) }
  }

  private fun handleBottomBarHeight(height: Int) {
    layoutRoot.updateLayoutParams<ViewGroup.MarginLayoutParams> { this.bottomMargin = height }
  }
}
