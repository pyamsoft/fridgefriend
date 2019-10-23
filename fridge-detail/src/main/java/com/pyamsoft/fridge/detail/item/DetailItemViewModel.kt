/*
 * Copyright 2019 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.fridge.detail.item

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.core.Preferences
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.detail.DetailPreferenceInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class DetailItemViewModel protected constructor(
    item: FridgeItem,
    interactor: DetailPreferenceInteractor,
    protected val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : UiViewModel<DetailItemViewState, DetailItemViewEvent, DetailItemControllerEvent>(
    initialState = DetailItemViewState(
        expirationRange = interactor.getExpiringSoonRange(),
        isSameDayExpired = interactor.isSameDayExpired(),
        throwable = null,
        item = item,
        sameNamedItems = emptyList(),
        similarItems = emptyList()
    )
) {

    init {
        var expiringSoonUnregister: Preferences.Unregister? = null

        doOnInit {
            expiringSoonUnregister = interactor.watchForExpiringSoonChanges { newRange ->
                setState { copy(expirationRange = newRange) }
            }
        }

        doOnTeardown {
            expiringSoonUnregister?.unregister()
            expiringSoonUnregister = null
        }

        var isSameDayExpiredUnregister: Preferences.Unregister? = null

        doOnInit {
            isSameDayExpiredUnregister = interactor.watchForSameDayExpiredChange { newSameDay ->
                setState { copy(isSameDayExpired = newSameDay) }
            }
        }

        doOnTeardown {
            isSameDayExpiredUnregister?.unregister()
            isSameDayExpiredUnregister = null
        }
    }

    private val deleteRunner = highlander<
        Unit,
        suspend (item: FridgeItem) -> Unit,
            (item: FridgeItem) -> Unit> { doRemove, onRemoved ->
        try {
            doRemove(item)
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error removing item: ${item.id()}")
            }
        } finally {
            onRemoved(item)
        }
    }

    private suspend fun handleFakeDelete(item: FridgeItem) {
        fakeRealtime.send(Delete(item))
    }

    @JvmOverloads
    protected fun remove(
        item: FridgeItem,
        doRemove: suspend (item: FridgeItem) -> Unit,
        onRemoved: (item: FridgeItem) -> Unit = {}
    ) = viewModelScope.launch {
        // If this item is not real, its an empty placeholder
        // The user may still wish to delete it from their list
        // in case they have too many placeholders.
        // Directly call the realtime delete callback as if the
        // delete had actually happened
        if (!item.isReal()) {
            Timber.w("Remove called on a non-real item: $item, fake callback")
            handleFakeDelete(item)
            return@launch
        }

        deleteRunner.call(doRemove, onRemoved)
    }
}
