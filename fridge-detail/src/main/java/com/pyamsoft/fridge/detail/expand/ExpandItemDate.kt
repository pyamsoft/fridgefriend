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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.base.BaseItemDate
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class ExpandItemDate @Inject internal constructor(
    imageLoader: ImageLoader,
    theming: ThemeProvider,
    parent: ViewGroup
) : BaseItemDate<ExpandItemViewState, ExpandedItemViewEvent>(imageLoader, theming, parent) {

    init {
        doOnInflate {
            layoutRoot.setOnDebouncedClickListener {
                publish(ExpandedItemViewEvent.PickDate)
            }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }
    }

    override fun onRender(state: ExpandItemViewState) {
        renderItem(state.item)
    }
}
