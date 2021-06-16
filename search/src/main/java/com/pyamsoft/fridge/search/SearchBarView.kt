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

package com.pyamsoft.fridge.search

import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.search.databinding.SearchBarBinding
import com.pyamsoft.fridge.ui.appbar.AppBarActivity
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiView
import javax.inject.Inject

class SearchBarView
@Inject
internal constructor(
    appBarSource: AppBarActivity,
) : UiView<DetailViewState, DetailViewEvent.ToolbarEvent.Search>() {

  private var delegate: UiEditTextDelegate? = null

  init {
    // Replace the app bar background during switcher presence
    doOnInflate {
      appBarSource.requireAppBar { appBar ->
        val inflater = LayoutInflater.from(appBar.context)
        val binding = SearchBarBinding.inflate(inflater, appBar)
        onCreate(binding)

        doOnTeardown { appBar.removeView(binding.searchbarRoot) }
      }
    }

    doOnTeardown {
      delegate?.handleTeardown()
      delegate = null
    }
  }

  private fun onCreate(binding: SearchBarBinding) {
    binding.searchbarRoot.isVisible = true
    delegate =
        UiEditTextDelegate.create(binding.searchbarName) { newText ->
          publish(DetailViewEvent.ToolbarEvent.Search.Query(newText))
          return@create true
        }
            .apply { handleCreate() }
  }

  override fun render(state: UiRender<DetailViewState>) {
    state.mapChanged { it.search }.render(viewScope) { handleSearch(it) }
  }

  private fun handleSearch(search: String) {
    requireNotNull(delegate).handleTextChanged(search)
  }
}
