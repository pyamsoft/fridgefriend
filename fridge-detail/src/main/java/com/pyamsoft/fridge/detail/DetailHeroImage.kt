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
import com.pyamsoft.fridge.core.view.HeroImage
import com.pyamsoft.fridge.db.item.FridgeItem.Presence.NEED
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class DetailHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader
) : HeroImage<DetailViewState, DetailViewEvent>(parent, imageLoader) {

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: DetailViewState
    ): Loaded? {
        val need = state.listItemPresence == NEED
        val icon = if (need) R.drawable.bg_item_need else R.drawable.bg_item_have
        return imageLoader.load(icon).into(imageView)
    }
}
