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
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.databinding.DetailEmptyBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailEmptyState @Inject internal constructor(
    parent: ViewGroup,
    private val imageLoader: ImageLoader
) : BaseUiView<DetailViewState, DetailViewEvent, DetailEmptyBinding>(parent) {

    override val viewBinding = DetailEmptyBinding::inflate

    override val layoutRoot by boundView { detailEmptyRoot }

    private var loaded: Loaded? = null

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        loaded?.dispose()
        loaded = null
        binding.detailEmptyMessage.text = null
    }

    override fun onRender(state: DetailViewState) {
        state.isLoading.let { loading ->
            if (loading == null) {
                clear()
            } else {
                if (loading.isLoading) {
                    clear()
                } else {
                    clear()
                    if (state.items.isEmpty()) {
                        val isNeed = state.listItemPresence == NEED
                        loadText(isNeed, state.search.isNotBlank())
                        loadImage(isNeed)
                    }
                }
            }
        }
    }

    private fun loadText(isNeed: Boolean, isSearch: Boolean) {
        val text = when {
            isSearch -> "Your search returned no results."
            isNeed -> "Your shopping list is empty, make a note about anything you need to buy!"
            else -> "Your fridge is empty, add items to your shopping list and go to the store!"
        }
        binding.detailEmptyMessage.text = text
    }

    private fun loadImage(isNeed: Boolean) {
        val icon = if (isNeed) R.drawable.bg_item_need else R.drawable.bg_item_have
        loaded = imageLoader.load(icon)
            .into(binding.detailEmptyImage)
    }
}
