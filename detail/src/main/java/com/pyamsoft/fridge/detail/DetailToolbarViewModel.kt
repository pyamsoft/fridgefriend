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

package com.pyamsoft.fridge.detail

import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailToolbarViewModel @AssistedInject internal constructor(
    private val delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) : UiSavedStateViewModel<DetailViewState, DetailViewEvent.ToolbarEvent, DetailToolbarControllerEvent>(
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

        viewModelScope.launch(context = Dispatchers.Default) {
            val sort = restoreSavedState(SAVED_SORT) { DetailViewState.Sorts.CREATED }
            updateSort(sort)
        }

        viewModelScope.launch(context = Dispatchers.Default) {
            val search = restoreSavedState(SAVED_SEARCH) { "" }
            if (search.isNotBlank()) {
                updateSearch(search)
            }
        }
    }

    override fun handleViewEvent(event: DetailViewEvent.ToolbarEvent) = when (event) {
        is DetailViewEvent.ToolbarEvent.Toolbar.Back -> handleBack()
        is DetailViewEvent.ToolbarEvent.Toolbar.ChangeSort -> updateSort(event.sort)
        is DetailViewEvent.ToolbarEvent.Search.Query -> updateSearch(event.search)
    }

    private fun updateSearch(search: String) {
        viewModelScope.updateSearch(search)
    }

    private fun CoroutineScope.updateSearch(search: String) {
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

    private fun updateSort(sort: DetailViewState.Sorts) {
        viewModelScope.updateSort(sort)
    }

    private fun CoroutineScope.updateSort(sort: DetailViewState.Sorts) {
        val scope = this

        delegate.apply {
            scope.updateSort(sort) { newState ->
                putSavedState(SAVED_SORT, newState.sort.name)
            }
        }
    }

    private fun handleBack() {
        publish(DetailToolbarControllerEvent.Back)
    }

    companion object {
        private const val SAVED_SORT = "sort"
        private const val SAVED_SEARCH = "search"
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<DetailToolbarViewModel> {
        override fun create(savedState: UiSavedState): DetailToolbarViewModel
    }
}
