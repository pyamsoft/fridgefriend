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
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiViewModel
import timber.log.Timber
import javax.inject.Inject

class DetailViewModel @Inject internal constructor(
    private val delegate: DetailListStateModel,
) : UiViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = delegate.initialState
) {

    init {
        val job = delegate.bind(Renderable { state ->
            state.render(viewModelScope) { newState ->
                setState { newState }
            }
        })
        doOnCleared {
            job.cancel()
        }
        doOnCleared {
            delegate.clear()
        }

        doOnSaveState { outState, state ->
            outState.put(SAVED_FILTER, state.showing.name)
        }

        doOnSaveState { outState, state ->
            outState.put(SAVED_SORT, state.sort.name)
        }

        doOnSaveState { outState, state ->
            state.search.let { search ->
                if (search.isNotBlank()) {
                    outState.put(SAVED_SEARCH, search)
                    return@doOnSaveState
                }
            }

            outState.remove(SAVED_SEARCH)
        }

        doOnRestoreState { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_FILTER) { filterName ->
                val filter = DetailViewState.Showing.valueOf(filterName)
                delegate.updateFilter(filter)
            }
        }

        doOnRestoreState { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_SORT) { sortName ->
                val sort = DetailViewState.Sorts.valueOf(sortName)
                delegate.updateSort(sort)
            }
        }

        doOnRestoreState { savedInstanceState ->
            savedInstanceState.useIfAvailable<String>(SAVED_SEARCH) { search ->
                delegate.updateSearch(search)
            }
        }
    }

    override fun handleViewEvent(event: DetailViewEvent) {
        return when (event) {
            is DetailViewEvent.ForceRefresh -> delegate.refreshList(true)
            is DetailViewEvent.ToggleArchiveVisibility -> delegate.toggleArchived()
            is DetailViewEvent.SearchQuery -> delegate.updateSearch(event.search)
            is DetailViewEvent.ChangeSort -> delegate.updateSort(event.sort)
            is DetailViewEvent.PresenceSwitched -> delegate.handlePresenceSwitch(event.presence)
            is DetailViewEvent.ReallyDeleteItemNoUndo -> delegate.deleteForever(event.item)
            is DetailViewEvent.UndoDeleteItem -> delegate.handleUndoDelete(event.item)
            is DetailViewEvent.ChangeItemPresence -> delegate.commitPresence(event.item)
            is DetailViewEvent.ConsumeItem -> delegate.consume(event.item)
            is DetailViewEvent.DeleteItem -> delegate.delete(event.item)
            is DetailViewEvent.RestoreItem -> delegate.restore(event.item)
            is DetailViewEvent.SpoilItem -> delegate.spoil(event.item)
            is DetailViewEvent.IncreaseItemCount -> delegate.increaseCount(event.item)
            is DetailViewEvent.DecreaseItemCount -> delegate.decreaseCount(event.item)
            is DetailViewEvent.AddNew -> handleAddNew()
            is DetailViewEvent.Back -> handleBack()
            is DetailViewEvent.ExpandItem -> handleExpand(event.item)
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

    private fun handleExpand(item: FridgeItem) {
        publish(ExpandForEditing(item))
    }

    companion object {
        private const val SAVED_SORT = "sort"
        private const val SAVED_FILTER = "filter"
        private const val SAVED_SEARCH = "search"
    }
}
