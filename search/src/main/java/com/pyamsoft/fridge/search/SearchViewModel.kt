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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.detail.DetailListStateModel
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope

class SearchViewModel @AssistedInject internal constructor(
    private val delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState
) : UiSavedStateViewModel<DetailViewState, DetailViewEvent.ToolbarEvent.Search, Nothing>(
    savedState, initialState = delegate.initialState
) {

    init {
        val job = delegate.bind(Renderable { state ->
            state.render(viewModelScope) { setState { it } }
        })
        doOnCleared {
            job.cancel()
        }
        doOnCleared {
            delegate.clear()
        }
    }

    override fun handleViewEvent(event: DetailViewEvent.ToolbarEvent.Search) = when (event) {
        is DetailViewEvent.ToolbarEvent.Search.Query -> handleUpdateSearch(event.search)
    }

    private fun handleUpdateSearch(search: String) {
        viewModelScope.handleUpdateSearch(search)
    }

    private fun CoroutineScope.handleUpdateSearch(search: String) {
        val scope = this
        delegate.apply {
            scope.updateSearch(search) { newState ->
                val newSearch = newState.search
                if (newSearch.isNotBlank()) {
                    putSavedState(SAVED_SEARCH, newSearch)
                } else {
                    removeSavedState(SAVED_SEARCH)
                }
            }
        }
    }

    companion object {
        private const val SAVED_SEARCH = "search"
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<SearchViewModel> {
        override fun create(savedState: UiSavedState): SearchViewModel
    }

}
