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

package com.pyamsoft.fridge.search

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.databinding.DetailEmptyBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class SearchEmptyState @Inject internal constructor(
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ListEvent, DetailEmptyBinding>(parent) {

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

    override fun onRender(state: UiRender<DetailViewState>) {
        state.render(viewScope) { handleLoading(it) }
    }

    private fun handleLoading(state: DetailViewState) {
        state.isLoading.let { loading ->
            clear()
            if (!loading) {
                if (state.displayedItems.isEmpty()) {
                    val isNeed = state.listItemPresence == NEED
                    loadText(isNeed, state.search.isNotBlank())
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
}
