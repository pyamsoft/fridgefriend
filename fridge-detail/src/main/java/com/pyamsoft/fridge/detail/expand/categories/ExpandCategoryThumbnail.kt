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

package com.pyamsoft.fridge.detail.expand.categories

import android.view.ViewGroup
import android.widget.ImageView
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class ExpandCategoryThumbnail @Inject internal constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader
) : BaseUiView<ExpandedCategoryViewState, ExpandedCategoryViewEvent>(parent) {

    override val layout: Int = R.layout.expand_category_thumbnail

    override val layoutRoot by boundView<ImageView>(R.id.expand_category_thumbnail)

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

    override fun onRender(state: ExpandedCategoryViewState) {
        state.category.let { category ->
            if (category?.thumbnail == null) {
                clear()
            } else {
                loadImage(category.thumbnail)
            }
        }
    }

    private fun loadImage(thumbnail: FridgeCategory.Thumbnail) {
        clear()
        loaded = imageLoader.load(thumbnail.data).into(layoutRoot)
    }
}
