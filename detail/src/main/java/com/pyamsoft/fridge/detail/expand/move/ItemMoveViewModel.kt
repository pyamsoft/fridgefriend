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

package com.pyamsoft.fridge.detail.expand.move

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ItemMoveViewModel @Inject internal constructor(
    private val interactor: DetailInteractor,
    private val itemId: FridgeItem.Id,
    private val entryId: FridgeEntry.Id,
) : UiViewModel<ItemMoveViewState, ItemMoveViewEvent, ItemMoveControllerEvent>(
    initialState = ItemMoveViewState(
        item = null
    )
) {

    private val itemResolveRunner = highlander<Unit, FridgeItem.Id> { resolveItemId ->
        val item = interactor.loadItem(
            resolveItemId,
            entryId,
            force = false
        )

        setState { copy(item = item) }
    }

    init {
        viewModelScope.launch(context = Dispatchers.Default) {
            itemResolveRunner.call(itemId)
        }
    }


    override fun handleViewEvent(event: ItemMoveViewEvent) {
        return when (event) {
            is ItemMoveViewEvent.Close -> closeDialog()
        }
    }

    private fun closeDialog() {
        publish(ItemMoveControllerEvent.Close)
    }

    fun handleMoveItemToEntry(entry: FridgeEntry) {
        state.item?.let { item ->
            Timber.d("Move item from ${item.entryId()} to ${entry.id()}")
            viewModelScope.launch(context = Dispatchers.Default) {
                Timber.d("Remove old item")
                interactor.delete(item, false)

                Timber.d("Create item with new entry id")
                interactor.commit(item.migrateTo(entry.id()))
            }
            closeDialog()
        }
    }
}
