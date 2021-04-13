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
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiSavedState

abstract class BaseExpandDetailViewModel<C : UiControllerEvent> protected constructor(
    private val delegate: DetailListStateModel,
    savedState: UiSavedState,
) : BaseDetailViewModel<C>(
    delegate, savedState
) {

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

    private inline fun withItemAt(index: Int, block: (FridgeItem) -> Unit) {
        block(state.displayedItems[index])
    }

    fun handleExpand(index: Int) {
        withItemAt(index) {
            onExpand(it)
        }
    }

    protected abstract fun onExpand(item: FridgeItem)
}
