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

package com.pyamsoft.fridge.search

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.search.databinding.SearchBarBinding
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject

class SearchBarView @Inject internal constructor(
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ToolbarEvent.Search, SearchBarBinding>(parent) {

    override val viewBinding = SearchBarBinding::inflate

    override val layoutRoot by boundView { searchbarRoot }

    private val delegate by lazy {
        UiEditTextDelegate(binding.searchbarName) { _, newText ->
            publish(DetailViewEvent.ToolbarEvent.Search.Query(newText))
        }
    }

    init {
        doOnTeardown {
            delegate.destroy()
        }

        doOnInflate {
            delegate.create()
        }
    }

    override fun onRender(state: UiRender<DetailViewState>) {
        state.mapChanged { it.search }.render(viewScope) { handleSearch(it) }
    }

    private fun handleSearch(search: String) {
        delegate.render(search)
    }

}
