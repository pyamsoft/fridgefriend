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

package com.pyamsoft.fridge.entry.item

import android.view.ViewGroup
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.fridge.entry.databinding.EntryItemScrimBinding
import com.pyamsoft.fridge.ui.intoBackground
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

class EntryListItemScrim @Inject internal constructor(
    private val imageLoader: ImageLoader,
    parent: ViewGroup,
) : BaseUiView<EntryItemViewState, EntryItemViewEvent, EntryItemScrimBinding>(parent) {

    override val viewBinding = EntryItemScrimBinding::inflate

    override val layoutRoot by boundView { entryItemScrim }

    private var loaded: Loaded? = null

    init {
        doOnTeardown {
            clear()
        }

        doOnInflate {
            layoutRoot.setOnDebouncedClickListener {
                publish(EntryItemViewEvent.ExpandEntry)
            }
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
        }
    }

    private fun clear() {
        loaded?.dispose()
        loaded = null
    }

    override fun onRender(state: UiRender<EntryItemViewState>) {
        state.distinctBy { it.itemCount }.render { handleScrim(it) }
    }

    private fun handleScrim(itemCount: Int) {
        clear()
        if (itemCount > 0) {
            loaded = imageLoader.load(R.drawable.entry_item_scrim).intoBackground(layoutRoot)
        }
    }

}
