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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailControllerEvent
import com.pyamsoft.fridge.detail.DetailInternalApi
import com.pyamsoft.fridge.detail.DetailListStateModel
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

class SearchListViewModel @Inject internal constructor(
    @DetailInternalApi private val delegate: DetailListStateModel,
) : UiViewModel<DetailViewState, DetailViewEvent.Main.ListEvent, DetailControllerEvent.Expand>(
    initialState = delegate.initialState
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

    override fun handleViewEvent(event: DetailViewEvent.Main.ListEvent) = when (event) {
        is DetailViewEvent.Main.ListEvent.ForceRefresh -> delegate.refreshList(true)
        is DetailViewEvent.Main.ListEvent.ChangeItemPresence -> delegate.commitPresence(event.index)
        is DetailViewEvent.Main.ListEvent.ConsumeItem -> delegate.consume(event.index)
        is DetailViewEvent.Main.ListEvent.DeleteItem -> delegate.delete(event.index)
        is DetailViewEvent.Main.ListEvent.RestoreItem -> delegate.restore(event.index)
        is DetailViewEvent.Main.ListEvent.SpoilItem -> delegate.spoil(event.index)
        is DetailViewEvent.Main.ListEvent.IncreaseItemCount -> delegate.increaseCount(event.index)
        is DetailViewEvent.Main.ListEvent.DecreaseItemCount -> delegate.decreaseCount(event.index)
        is DetailViewEvent.Main.ListEvent.ExpandItem -> handleExpand(event.index)
    }

    private inline fun withItemAt(index: Int, block: (FridgeItem) -> Unit) {
        block(state.displayedItems[index])
    }

    private fun handleExpand(index: Int) {
        withItemAt(index) { publish(DetailControllerEvent.Expand.ExpandForEditing(it)) }
    }
}
