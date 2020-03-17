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

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.core.HeroImage
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader
) : HeroImage<DetailViewState, DetailViewEvent>(parent, imageLoader) {

    init {
        doOnTeardown {
            clearExtras()
        }
    }

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: DetailViewState
    ): Loaded? {
        val need = state.listItemPresence == FridgeItem.Presence.NEED
        val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
        return imageLoader.load(icon).into(imageView)
    }

    @SuppressLint("SetTextI18n")
    private fun setTitle(state: DetailViewState) {
        val type = when (state.listItemPresence) {
            FridgeItem.Presence.HAVE -> "current"
            FridgeItem.Presence.NEED -> "needed"
        }
        binding.coreHeroTitle.text = "${state.entry?.name().orEmpty()}: $type items"
    }

    override fun onAdditionalRender(state: DetailViewState) {
        setTitle(state)

        binding.coreHeroFirstLineLabel.setText(R.string.total_number_of_items)
        binding.coreHeroFirstLineValue.text = "${state.getTotalItemCount()}"

        val showExtras = state.listItemPresence == FridgeItem.Presence.HAVE
        binding.coreHeroSecondLineLabel.isVisible = showExtras
        binding.coreHeroSecondLineValue.isVisible = showExtras
        binding.coreHeroThirdLineLabel.isVisible = showExtras
        binding.coreHeroThirdLineValue.isVisible = showExtras
        binding.coreHeroFourthLineLabel.isVisible = showExtras
        binding.coreHeroFourthLineValue.isVisible = showExtras

        if (showExtras) {
            binding.coreHeroSecondLineLabel.setText(R.string.number_of_fresh_items)
            binding.coreHeroSecondLineValue.text = "${state.getFreshItemCount()}"

            binding.coreHeroThirdLineLabel.setText(R.string.number_of_consumed_items)
            binding.coreHeroThirdLineValue.text = "${state.getConsumedItemCount()}"

            binding.coreHeroFourthLineLabel.setText(R.string.number_of_spoiled_items)
            binding.coreHeroFourthLineValue.text = "${state.getSpoiledItemCount()}"
        } else {
            clearExtras()
        }
    }

    private fun clearExtras() {
        binding.coreHeroSecondLineLabel.text = null
        binding.coreHeroSecondLineValue.text = null

        binding.coreHeroThirdLineLabel.text = null
        binding.coreHeroThirdLineValue.text = null

        binding.coreHeroFourthLineLabel.text = null
        binding.coreHeroFourthLineValue.text = null
    }
}
