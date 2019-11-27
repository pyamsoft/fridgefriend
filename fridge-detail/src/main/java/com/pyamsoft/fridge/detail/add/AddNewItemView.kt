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

package com.pyamsoft.fridge.detail.add

import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.popHide
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class AddNewItemView @Inject internal constructor(
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseUiView<DetailViewState, DetailViewEvent>(parent) {

    override val layout: Int = R.layout.add_new

    override val layoutRoot by boundView<FloatingActionButton>(R.id.detail_add_new_item)

    private var iconLoaded: Loaded? = null

    init {
        doOnInflate {
            iconLoaded = imageLoader.load(R.drawable.ic_add_24dp)
                .into(layoutRoot)

            layoutRoot.setOnDebouncedClickListener { publish(DetailViewEvent.AddNewItemEvent) }
            layoutRoot.popShow()
        }

        doOnTeardown {
            disposeIcon()
            layoutRoot.setOnClickListener(null)
        }
    }

    override fun onRender(
        state: DetailViewState,
        savedState: UiSavedState
    ) {
        state.actionVisible?.let { action ->
            if (action.visible) {
                layoutRoot.popShow()
            } else {
                layoutRoot.popHide()
            }
        }
    }

    private fun disposeIcon() {
        iconLoaded?.dispose()
        iconLoaded = null
    }
}
