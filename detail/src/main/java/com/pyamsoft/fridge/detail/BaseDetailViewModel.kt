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
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseDetailViewModel protected constructor(
    private val delegate: DetailListStateModel,
    savedState: UiSavedState,
) : UiSavedStateViewModel<DetailViewState, DetailViewEvent.ListEvent, UnitControllerEvent>(
    savedState, initialState = delegate.initialState
) {

    init {
        val scope = viewModelScope
        val job = delegate.bindState(scope, Renderable { state ->
            state.render(scope) { scope.setState { it } }
        })
        doOnCleared {
            job.cancel()
        }
        doOnCleared {
            delegate.clear()
        }

        delegate.initialize(scope)

        viewModelScope.launch(context = Dispatchers.Default) {
            val filterName = restoreSavedState(SAVED_FILTER) { "" }
            if (filterName.isNotBlank()) {
                val filter = DetailViewState.Showing.valueOf(filterName)
                delegate.handleUpdateFilter(this, filter)
            }
        }
    }

    fun handleRefreshList(force: Boolean) {
        delegate.handleRefreshList(viewModelScope, force)
    }

    fun handleCommitPresence(index: Int) {
        delegate.handleCommitPresence(viewModelScope, index)
    }

    fun handleDelete(index: Int) {
        delegate.handleDelete(viewModelScope, index)
    }

    fun handleConsume(index: Int) {
        delegate.handleConsume(viewModelScope, index)
    }

    fun handleRestore(index: Int) {
        delegate.handleRestore(viewModelScope, index)
    }

    fun handleSpoil(index: Int) {
        delegate.handleSpoil(viewModelScope, index)
    }

    fun handleIncreaseCount(index: Int) {
        delegate.handleIncreaseCount(viewModelScope, index)
    }

    fun handleDecreaseCount(index: Int) {
        delegate.handleDecreaseCount(viewModelScope, index)
    }

    fun handleClearListError() {
        delegate.handleClearListError(viewModelScope)
    }

    fun handleAddAgain(item: FridgeItem) {
        delegate.handleAddAgain(viewModelScope, item)
    }

    fun handleDeleteForever() {
        delegate.handleDeleteForever(viewModelScope)
    }

    fun handleUndoDelete() {
        delegate.handleUndoDelete(viewModelScope)
    }

    fun handleUpdateShowing() {
        delegate.handleToggleArchived(viewModelScope) { newShowing ->
            putSavedState(SAVED_FILTER, newShowing.name)
        }
    }

    private inline fun withItemAt(index: Int, block: (FridgeItem) -> Unit) {
        block(state.displayedItems[index])
    }

    fun handleExpand(index: Int, onExpand: (FridgeItem) -> Unit) {
        withItemAt(index) { onExpand(it) }
    }

    companion object {
        private const val SAVED_FILTER = "filter"
    }
}
