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

package com.pyamsoft.fridge.detail.expand.categories

import android.view.ViewGroup
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.databinding.ExpandCategoryScrimBinding
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class ExpandCategoryScrim @Inject internal constructor(
    parent: ViewGroup,
) : ExpandCategoryClickable<ExpandCategoryScrimBinding>(parent) {

    override val viewBinding = ExpandCategoryScrimBinding::inflate

    override val layoutRoot by boundView { expandCategoryScrim }

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        layoutRoot.isVisible = false
    }

    override fun onRender(state: UiRender<ExpandedCategoryViewState>) {
        state.distinctBy { it.category }.render(viewScope) { handleCategory(it) }
    }

    private fun handleCategory(category: ExpandedCategoryViewState.Category?) {
        if (category == null || category.name.isBlank()) {
            clear()
        } else {
            layoutRoot.isVisible = true
        }
    }
}
