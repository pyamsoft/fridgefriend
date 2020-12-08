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
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.detail.databinding.ExpandCategoryThumbnailBinding
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class ExpandCategoryThumbnail @Inject internal constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader,
) : ExpandCategoryClickable<ExpandCategoryThumbnailBinding>(parent) {

    override val viewBinding = ExpandCategoryThumbnailBinding::inflate

    override val layoutRoot by boundView { expandCategoryThumbnail }

    private var loaded: Loaded? = null

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        loaded?.dispose()
        loaded = null
    }

    override fun onRender(state: UiRender<ExpandedCategoryViewState>) {
        state.distinctBy { it.category }.render { handleCategory(it) }
    }

    private fun handleCategory(category: ExpandedCategoryViewState.Category?) {
        clear()
        if (category?.thumbnail != null) {
            loadImage(category.thumbnail)
        }
    }

    private fun loadImage(thumbnail: FridgeCategory.Thumbnail) {
        loaded = imageLoader.load(thumbnail.data).into(layoutRoot)
    }
}
