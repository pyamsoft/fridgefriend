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
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailBackground @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader,
    listItemPresence: FridgeItem.Presence
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.detail_background

    override val layoutRoot by boundView<ViewGroup>(R.id.detail_background_root)
    private val image by boundView<ImageView>(R.id.detail_background_image)
    private val text by boundView<TextView>(R.id.detail_background_text)

    private val need = listItemPresence == NEED

    init {
        var loaded: Loaded? = null
        doOnInflate {
            val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
            loaded = imageLoader.load(icon)
                .into(image)
        }

        doOnTeardown {
            loaded?.dispose()
        }

        doOnTeardown {
            text.text = null
        }
    }

    override fun onRender(state: DetailViewState, savedState: UiSavedState) {
        state.items.let { items ->
            when {
                items == null || items.isNotEmpty() -> {
                    text.text = null
                    text.isInvisible = true
                }
                items.isEmpty() -> {
                    val message: String
                    val which = if (need) "Your shopping list is empty" else "Your fridge is empty"
                    message = "${which}, click the plus to get started"

                    text.text = message
                    text.isVisible = true
                }
            }
        }
    }
}
