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
import com.pyamsoft.fridge.detail.databinding.ExpandErrorBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class ExpandItemError
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<ExpandedViewState, ExpandedViewEvent, ExpandErrorBinding>(parent) {

  override val viewBinding = ExpandErrorBinding::inflate

  override val layoutRoot by boundView { expandItemErrorRoot }

  init {
    doOnInflate {
      // No errors initially right
      binding.expandItemErrorMsg.isVisible = false
    }

    doOnTeardown { clear() }
  }

  private fun clear() {
    binding.expandItemErrorMsg.isVisible = false
    binding.expandItemErrorMsg.text = ""
  }

  private fun handleError(throwable: Throwable?) {
    if (throwable == null) {
      clear()
    } else {
      binding.expandItemErrorMsg.isVisible = true
      binding.expandItemErrorMsg.text = throwable.message ?: "An unknown error occurred"
    }
  }

  override fun onRender(state: UiRender<ExpandedViewState>) {
    state.mapChanged { it.throwable }.render(viewScope) { handleError(it) }
  }
}
