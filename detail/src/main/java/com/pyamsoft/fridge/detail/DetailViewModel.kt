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

package com.pyamsoft.fridge.detail

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class DetailViewModel @AssistedInject internal constructor(
    @DetailInternalApi delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) : DelegatedDetailViewModel<DetailViewEvent.Main, DetailControllerEvent>(
    savedState, delegate
) {

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            val filterName = restoreSavedState(SAVED_FILTER) { "" }
            if (filterName.isNotBlank()) {
                val filter = DetailViewState.Showing.valueOf(filterName)
                delegate.updateFilter(filter)
            }
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

    override fun handleViewEvent(event: DetailViewEvent.Main) = when (event) {
        is DetailViewEvent.Main.ListEvent.ForceRefresh -> delegate.refreshList(true)
        is DetailViewEvent.Main.ListEvent.ChangeItemPresence -> delegate.commitPresence(event.index)
        is DetailViewEvent.Main.ListEvent.ConsumeItem -> delegate.consume(event.index)
        is DetailViewEvent.Main.ListEvent.DeleteItem -> delegate.delete(event.index)
        is DetailViewEvent.Main.ListEvent.RestoreItem -> delegate.restore(event.index)
        is DetailViewEvent.Main.ListEvent.SpoilItem -> delegate.spoil(event.index)
        is DetailViewEvent.Main.ListEvent.IncreaseItemCount -> delegate.increaseCount(event.index)
        is DetailViewEvent.Main.ListEvent.DecreaseItemCount -> delegate.decreaseCount(event.index)
        is DetailViewEvent.Main.ListEvent.ExpandItem -> handleExpand(event.index)
        is DetailViewEvent.Main.AddEvent.ToggleArchiveVisibility -> updateShowing()
        is DetailViewEvent.Main.AddEvent.ReallyDeleteItemNoUndo -> delegate.reallyDelete()
        is DetailViewEvent.Main.AddEvent.UndoDeleteItem -> delegate.handleUndoDelete()
        is DetailViewEvent.Main.AddEvent.ClearListError -> delegate.clearListError()
        is DetailViewEvent.Main.AddEvent.AddNew -> handleAddNew()
        is DetailViewEvent.Main.ToolbarEvent.Back -> handleBack()
        is DetailViewEvent.Main.ToolbarEvent.SearchQuery -> updateSearch(event.search)
        is DetailViewEvent.Main.ToolbarEvent.ChangeSort -> updateSort(event.sort)
    }

    private fun updateSearch(search: String) {
        delegate.updateSearch(search) { newState ->
            val newSearch = newState.search
            if (newSearch.isNotBlank()) {
                putSavedState(SAVED_SEARCH, newSearch)
            } else {
                removeSavedState(SAVED_SEARCH)
            }
        }
    }

    private fun updateShowing() {
        delegate.toggleArchived { newState ->
            putSavedState(SAVED_FILTER, newState.showing.name)
        }
    }

    private fun updateSort(sort: DetailViewState.Sorts) {
        delegate.updateSort(sort) { newState ->
            putSavedState(SAVED_SORT, newState.sort.name)
        }
    }

    private fun handleBack() {
        publish(DetailControllerEvent.Back)
    }

    private fun handleAddNew() {
        state.apply {
            val e = entry
            if (e == null) {
                Timber.w("Cannot add new, detail entry null!")
            } else {
                publish(DetailControllerEvent.AddNew(e.id(), listItemPresence))
            }
        }
    }

    private inline fun withItemAt(index: Int, block: (FridgeItem) -> Unit) {
        block(state.displayedItems[index])
    }

    private fun handleExpand(index: Int) {
        withItemAt(index) { publish(ExpandForEditing(it)) }
    }

    companion object {
        private const val SAVED_SORT = "sort"
        private const val SAVED_FILTER = "filter"
        private const val SAVED_SEARCH = "search"
    }

    @AssistedInject.Factory
    interface Factory : UiSavedStateViewModelProvider<DetailViewModel> {
        override fun create(savedState: UiSavedState): DetailViewModel
    }
}
