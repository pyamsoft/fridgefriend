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

package com.pyamsoft.fridge.detail.item

import android.view.ViewGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.databinding.DetailListItemCountBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import com.pyamsoft.pydroid.util.tintWith
import timber.log.Timber
import javax.inject.Inject
import com.pyamsoft.pydroid.ui.R as R2

class DetailListItemCount @Inject internal constructor(
    theming: ThemeProvider,
    imageLoader: ImageLoader,
    parent: ViewGroup,
) : BaseUiView<DetailItemViewState, DetailItemViewEvent, DetailListItemCountBinding>(parent) {

    override val viewBinding = DetailListItemCountBinding::inflate

    override val layoutRoot by boundView { detailItemCount }

    init {
        doOnInflate {
            imageLoader.load(R.drawable.ic_arrow_drop_down_24dp)
                .mutate { icon ->
                    val color = if (theming.isDarkTheme()) R2.color.white else R2.color.black
                    icon.tintWith(layoutRoot.context, color)
                }.into(binding.detailItemCountDown)
                .apply { doOnTeardown { dispose() } }

            imageLoader.load(R.drawable.ic_arrow_drop_up_24dp)
                .mutate { icon ->
                    val color = if (theming.isDarkTheme()) R2.color.white else R2.color.black
                    icon.tintWith(layoutRoot.context, color)
                }.into(binding.detailItemCountUp)
                .apply { doOnTeardown { dispose() } }
        }

        doOnInflate {
            binding.detailItemCountUp.setOnDebouncedClickListener {
                publish(DetailItemViewEvent.IncreaseCount)
            }

            binding.detailItemCountDown.setOnDebouncedClickListener {
                publish(DetailItemViewEvent.DecreaseCount)
            }

            binding.detailItemCountText.setOnDebouncedClickListener {
                Timber.d("Count text eat click")
            }
        }

        doOnTeardown {
            binding.detailItemCountUp.setOnDebouncedClickListener(null)
            binding.detailItemCountDown.setOnDebouncedClickListener(null)
            binding.detailItemCountText.setOnDebouncedClickListener(null)
        }
    }

    override fun onRender(state: UiRender<DetailItemViewState>) {
        state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
    }

    private fun handleItem(item: FridgeItem) {
        require(item.isReal()) { "Cannot render non-real item: $item" }
        binding.detailItemCountText.text = "${item.count()}"
    }
}
