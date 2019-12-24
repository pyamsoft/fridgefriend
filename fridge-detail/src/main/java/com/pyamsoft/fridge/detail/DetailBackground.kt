/*
 * Copyright 2019 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.withStyledAttributes
import androidx.core.view.updatePadding
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import javax.inject.Inject

class DetailBackground @Inject internal constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.detail_background

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_background_root)
    private val collapse by boundView<CollapsingToolbarLayout>(R.id.detail_background_collapse)
    private val image by boundView<ImageView>(R.id.detail_background_image)

    private var loaded: Loaded? = null

    init {
        doOnInflate {
            collapse.doOnApplyWindowInsets { v, insets, padding ->
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

    override fun onRender(state: DetailViewState, savedState: UiSavedState) {
        val need = state.listItemPresence == NEED
        loadImage(need)
    }

    private fun loadImage(need: Boolean) {
        clear()

        val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
        loaded = imageLoader.load(icon)
            .into(image)
    }
}
