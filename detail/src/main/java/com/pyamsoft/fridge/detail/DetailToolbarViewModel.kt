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
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DetailToolbarViewModel
@AssistedInject
internal constructor(
    private val delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) :
    UiSavedStateViewModel<DetailViewState, UnitControllerEvent>(
        savedState, initialState = delegate.initialState) {

  init {
    val scope = viewModelScope
    val job =
        delegate.bindState(
            scope, Renderable { state -> state.render(scope) { scope.setState { it } } })
    doOnCleared { job.cancel() }
    doOnCleared { delegate.clear() }

    delegate.initialize(scope)

    viewModelScope.launch(context = Dispatchers.Default) {
      val sort = restoreSavedState(SAVED_SORT) { DetailViewState.Sorts.CREATED }
      handleUpdateSort(sort)
    }

    viewModelScope.launch(context = Dispatchers.Default) {
      val search = restoreSavedState(SAVED_SEARCH) { "" }
      if (search.isNotBlank()) {
        handleUpdateSearch(search)
      }
    }
  }

  fun handleUpdateSearch(search: String) {
    delegate.handleUpdateSearch(viewModelScope, search) { newSearch ->
      if (newSearch.isNotBlank()) {
        putSavedState(SAVED_SEARCH, newSearch)
      } else {
        removeSavedState(SAVED_SEARCH)
      }
    }
  }

  fun handleUpdateSort(sort: DetailViewState.Sorts) {
    delegate.handleUpdateSort(viewModelScope, sort) { newSort ->
      putSavedState(SAVED_SORT, newSort.name)
    }
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
