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
 *
 */

package com.pyamsoft.fridge.core

import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.CheckResult
import androidx.core.content.withStyledAttributes
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.core.databinding.CoreHeroImageBinding
import com.pyamsoft.pydroid.arch.BindingUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets

abstract class HeroImage<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader
) : BindingUiView<S, V, CoreHeroImageBinding>(parent) {

    override val viewBinding = CoreHeroImageBinding::inflate

    override val layoutRoot by boundView { coreHeroRoot }

    private var loaded: Loaded? = null

    init {
        doOnInflate {
            binding.coreHeroCollapse.doOnApplyWindowInsets { v, insets, padding ->
                val toolbarTopMargin = padding.top + insets.systemWindowInsetTop
                v.context.withStyledAttributes(
                    R.attr.toolbarStyle,
                    intArrayOf(R.attr.actionBarSize)
                ) {
                    val sizeId = getResourceId(0, 0)
                    if (sizeId != 0) {
                        val toolbarHeight = v.context.resources.getDimensionPixelSize(sizeId)
                        v.updatePadding(top = toolbarTopMargin + toolbarHeight)
                    }
                }
            }
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        loaded?.dispose()
        loaded = null
    }

    final override fun onRender(state: S) {
        loadImage(state)
    }

    private fun loadImage(state: S) {
        clear()
        loaded = onLoadImage(binding.coreHeroImage, imageLoader, state)
    }

    @CheckResult
    protected abstract fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: S
    ): Loaded?
}
