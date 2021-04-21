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

package com.pyamsoft.fridge.category

import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.view.UiHeroImage
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UnitViewEvent
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import javax.inject.Inject

class CategoryHeroImage @Inject internal constructor(
    parent: ViewGroup,
    imageLoader: ImageLoader,
    owner: LifecycleOwner,
    appBarActivity: AppBarActivity,
) : UiHeroImage<CategoryViewState, UnitViewEvent>(parent, owner, appBarActivity, imageLoader) {

    override fun onLoadImage(
        imageView: ImageView,
        imageLoader: ImageLoader,
        state: CategoryViewState,
    ): Loaded? {
        return null
    }

    override fun onAdditionalRender(state: UiRender<CategoryViewState>) {
    }

}
