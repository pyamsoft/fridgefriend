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

package com.pyamsoft.fridge.detail

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.view.UiHeroImage
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader,
    owner: LifecycleOwner,
    appBarActivity: AppBarActivity,
) : UiHeroImage<DetailViewState, Nothing>(parent, owner, appBarActivity, imageLoader) {

    init {
        doOnTeardown {
            clearExtras()
        }
    }

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: DetailViewState,
    ): Loaded {
        val need = state.listItemPresence == FridgeItem.Presence.NEED
        val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
        return imageLoader.load(icon).into(imageView)
    }

    @SuppressLint("SetTextI18n")
    private fun handleTitle(state: DetailViewState) {
        val type = when (state.showing) {
            DetailViewState.Showing.FRESH -> when (state.listItemPresence) {
                FridgeItem.Presence.HAVE -> "current"
                FridgeItem.Presence.NEED -> "needed"
            }
            DetailViewState.Showing.CONSUMED -> "consumed"
            DetailViewState.Showing.SPOILED -> "spoiled"
        }
        binding.coreHeroTitle.text = "${state.entry?.name().orEmpty()}: $type items"
    }

    override fun onAdditionalRender(state: UiRender<DetailViewState>) {
        state.render(viewScope) { handleTitle(it) }
        state.render(viewScope) { handleShowing(it) }
    }

    private fun handleShowing(state: DetailViewState) {
        when (state.showing) {
            DetailViewState.Showing.FRESH -> renderFresh(state)
            DetailViewState.Showing.CONSUMED -> renderConsumed(state)
            DetailViewState.Showing.SPOILED -> renderSpoiled(state)
        }
    }

    private fun renderSpoiled(state: DetailViewState) {
        binding.coreHeroFirstLineLabel.isVisible = false
        binding.coreHeroFirstLineValue.isVisible = false
        binding.coreHeroSecondLineLabel.isVisible = false
        binding.coreHeroSecondLineValue.isVisible = false
        binding.coreHeroThirdLineLabel.isVisible = false
        binding.coreHeroThirdLineValue.isVisible = false
        binding.coreHeroFourthLineLabel.isVisible = false
        binding.coreHeroFourthLineValue.isVisible = false
    }

    private fun renderConsumed(state: DetailViewState) {
        binding.coreHeroFirstLineLabel.isVisible = false
        binding.coreHeroFirstLineValue.isVisible = false
        binding.coreHeroSecondLineLabel.isVisible = false
        binding.coreHeroSecondLineValue.isVisible = false
        binding.coreHeroThirdLineLabel.isVisible = false
        binding.coreHeroThirdLineValue.isVisible = false
        binding.coreHeroFourthLineLabel.isVisible = false
        binding.coreHeroFourthLineValue.isVisible = false
    }

    private fun renderFresh(state: DetailViewState) {
        binding.coreHeroFirstLineLabel.setText(R.string.total_number_of_items)
        binding.coreHeroFirstLineLabel.isVisible = true
        binding.coreHeroFirstLineValue.isVisible = true

        val showExtras = state.listItemPresence == FridgeItem.Presence.HAVE
        binding.coreHeroSecondLineLabel.isVisible = showExtras
        binding.coreHeroSecondLineValue.isVisible = showExtras
        binding.coreHeroThirdLineLabel.isVisible = showExtras
        binding.coreHeroThirdLineValue.isVisible = showExtras
        binding.coreHeroFourthLineLabel.isVisible = showExtras
        binding.coreHeroFourthLineValue.isVisible = showExtras

        if (showExtras) {
            binding.coreHeroSecondLineLabel.setText(R.string.number_of_fresh_items)
            binding.coreHeroThirdLineLabel.setText(R.string.number_of_expiring_items)
            binding.coreHeroFourthLineLabel.setText(R.string.number_of_expired_items)
        } else {
            clearExtras()
        }

        state.counts.let { counts ->
            if (counts != null) {
                binding.coreHeroFirstLineValue.text = "${counts.totalCount}"
                binding.coreHeroSecondLineValue.text = "${counts.firstCount}"
                binding.coreHeroThirdLineValue.text = "${counts.secondCount}"
                binding.coreHeroFourthLineValue.text = "${counts.thirdCount}"
            } else {
                binding.coreHeroFirstLineValue.text = null
                binding.coreHeroSecondLineValue.text = null
                binding.coreHeroThirdLineValue.text = null
                binding.coreHeroFourthLineValue.text = null
            }
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
