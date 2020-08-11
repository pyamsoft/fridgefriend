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

package com.pyamsoft.fridge.category

import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.ui.HeroImage
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class CategoryHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader
) : HeroImage<CategoryViewState, CategoryViewEvent>(parent, imageLoader) {

    init {
        doOnInflate {
            binding.coreHeroSecondLineLabel.isVisible = false
            binding.coreHeroSecondLineValue.isVisible = false
            binding.coreHeroThirdLineLabel.isVisible = false
            binding.coreHeroThirdLineValue.isVisible = false
            binding.coreHeroFourthLineLabel.isVisible = false
            binding.coreHeroFourthLineValue.isVisible = false
        }
    }

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: CategoryViewState
    ): Loaded? {
        // TODO
        return null
    }

    override fun onAdditionalRender(state: CategoryViewState) {
        binding.coreHeroTitle.setText(R.string.categories)

        binding.coreHeroFirstLineLabel.setText(R.string.total_number_of_categories)
        binding.coreHeroFirstLineValue.text = "${state.categories.size}"
    }
}