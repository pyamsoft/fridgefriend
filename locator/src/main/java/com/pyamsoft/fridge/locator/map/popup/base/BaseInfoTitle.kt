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

package com.pyamsoft.fridge.locator.map.popup.base

import android.view.ViewGroup
import com.pyamsoft.fridge.locator.R
import com.pyamsoft.fridge.locator.databinding.PopupInfoTitleBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

abstract class BaseInfoTitle<S : BaseInfoViewState, E : UiViewEvent> protected constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
) : BaseUiView<S, E, PopupInfoTitleBinding>(parent) {

    final override val viewBinding = PopupInfoTitleBinding::inflate

    final override val layoutRoot by boundView { popupInfoTitleRoot }

    private var favoriteLoaded: Loaded? = null

    init {
        doOnTeardown {
            binding.popupInfoTitle.text = ""
            binding.popupInfoFavorite.setOnDebouncedClickListener(null)
        }

        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        favoriteLoaded?.dispose()
        favoriteLoaded = null
    }

    private fun handleCached(cached: BaseInfoViewState.Cached?) {
        clear()
        if (cached == null) {
            binding.popupInfoFavorite.setOnDebouncedClickListener(null)
        } else {
            val isCached = cached.cached
            val icon = if (isCached) R.drawable.ic_star_24dp else R.drawable.ic_star_empty_24dp
            favoriteLoaded = imageLoader.load(icon).into(binding.popupInfoFavorite)
            binding.popupInfoFavorite.setOnDebouncedClickListener { publishFavorite(!isCached) }
        }
    }

    private fun handleName(name: String) {
        binding.popupInfoTitle.text = name
    }

    final override fun onRender(state: UiRender<S>) {
        state.distinctBy { it.name }.render(viewScope) { handleName(it) }
        state.distinctBy { it.cached }.render(viewScope) { handleCached(it) }
    }

    protected abstract fun publishFavorite(add: Boolean)
}
