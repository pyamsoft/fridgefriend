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
import com.pyamsoft.fridge.db.item.FridgeItem
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
import timber.log.Timber

class DetailViewModel @AssistedInject internal constructor(
    private val delegate: DetailListStateModel,
    @Assisted savedState: UiSavedState,
) : UiSavedStateViewModel<DetailViewState, DetailViewEvent.ListEvent, DetailControllerEvent>(
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
            val filterName = restoreSavedState(SAVED_FILTER) { "" }
            if (filterName.isNotBlank()) {
                val filter = DetailViewState.Showing.valueOf(filterName)
                handleUpdateFilter(filter)
            }
        }
    }

    override fun handleViewEvent(event: DetailViewEvent.ListEvent) = when (event) {
        is DetailViewEvent.ListEvent.ForceRefresh -> delegate.refreshList(true)
        is DetailViewEvent.ListEvent.ChangeItemPresence -> delegate.commitPresence(event.index)
        is DetailViewEvent.ListEvent.ConsumeItem -> delegate.consume(event.index)
        is DetailViewEvent.ListEvent.DeleteItem -> delegate.delete(event.index)
        is DetailViewEvent.ListEvent.RestoreItem -> delegate.restore(event.index)
        is DetailViewEvent.ListEvent.SpoilItem -> delegate.spoil(event.index)
        is DetailViewEvent.ListEvent.IncreaseItemCount -> delegate.increaseCount(event.index)
        is DetailViewEvent.ListEvent.DecreaseItemCount -> delegate.decreaseCount(event.index)
        is DetailViewEvent.ListEvent.ExpandItem -> handleExpand(event.index)
        is DetailViewEvent.ListEvent.ChangeCurrentFilter -> updateShowing()
        is DetailViewEvent.ListEvent.ReallyDeleteItemNoUndo -> delegate.reallyDelete()
        is DetailViewEvent.ListEvent.UndoDeleteItem -> delegate.handleUndoDelete()
        is DetailViewEvent.ListEvent.ClearListError -> delegate.clearListError()
        is DetailViewEvent.ListEvent.AddNew -> handleAddNew()
    }

    private fun updateShowing() {
        delegate.toggleArchived { newState ->
            putSavedState(SAVED_FILTER, newState.showing.name)
        }
    }

    private fun CoroutineScope.handleUpdateFilter(filter: DetailViewState.Showing) {
        val scope = this

        delegate.apply {
            scope.updateFilter(filter)
        }
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
        withItemAt(index) { publish(DetailControllerEvent.Expand.ExpandForEditing(it)) }
    }

    companion object {
        private const val SAVED_FILTER = "filter"
    }

    @AssistedFactory
    interface Factory : UiSavedStateViewModelProvider<DetailViewModel> {
        override fun create(savedState: UiSavedState): DetailViewModel
    }
}
