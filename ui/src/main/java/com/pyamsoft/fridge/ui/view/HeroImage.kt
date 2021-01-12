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

package com.pyamsoft.fridge.ui.view

import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.applyToolbarOffset
import com.pyamsoft.fridge.ui.databinding.UiHeroImageBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded

abstract class HeroImage<S : UiViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup,
    owner: LifecycleOwner,
    appBarActivity: AppBarActivity,
    private val imageLoader: ImageLoader,
) : BaseUiView<S, V, UiHeroImageBinding>(parent) {

    override val viewBinding = UiHeroImageBinding::inflate

    override val layoutRoot by boundView { coreHeroRoot }

    private var loaded: Loaded? = null

    init {
        doOnInflate {
            binding.coreHeroCollapse.applyToolbarOffset(appBarActivity, owner)
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        loaded?.dispose()
        loaded = null
    }

    override fun onRender(state: UiRender<S>) {
        state.render(viewScope) { loadImage(it) }
        onAdditionalRender(state)
    }

    private fun loadImage(state: S) {
        clear()
        loaded = onLoadImage(binding.coreHeroImage, imageLoader, state)
    }

    protected open fun onAdditionalRender(state: UiRender<S>) {
    }

    @CheckResult
    protected abstract fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: S,
    ): Loaded?
}
