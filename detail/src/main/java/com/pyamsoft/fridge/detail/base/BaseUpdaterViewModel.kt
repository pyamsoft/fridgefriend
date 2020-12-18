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

package com.pyamsoft.fridge.detail.base

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailInteractor
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.arch.onActualError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseUpdaterViewModel<S : UiViewState, V : UiViewEvent, C : UiControllerEvent> protected constructor(
    initialState: S,
) : UiViewModel<S, V, C>(initialState) {

    @CheckResult
    internal fun createUpdateDelegate(
        interactor: DetailInteractor,
        onError: (Throwable) -> Unit,
    ): UpdateDelegate {
        return createUpdateDelegate(viewModelScope, interactor, onError)
    }

}

@CheckResult
internal fun createUpdateDelegate(
    scope: CoroutineScope,
    interactor: DetailInteractor,
    onError: (Throwable) -> Unit,
): UpdateDelegate {
    return UpdateDelegate(scope, interactor, onError)
}

internal class UpdateDelegate internal constructor(
    viewModelScope: CoroutineScope,
    interactor: DetailInteractor,
    handleError: (Throwable) -> Unit,
) {

    private var viewModelScope: CoroutineScope? = viewModelScope
    private var interactor: DetailInteractor? = interactor
    private var handleError: ((Throwable) -> Unit)? = handleError

    fun teardown() {
        viewModelScope = null
        interactor = null
        handleError = null
    }

    private val updateRunner = highlander<
            Unit,
            FridgeItem,
            suspend (item: FridgeItem) -> Unit,
                (throwable: Throwable) -> Unit
            > { item, doUpdate, onError ->
        try {
            doUpdate(item.makeReal())
        } catch (error: Throwable) {
            error.onActualError { e ->
                Timber.e(e, "Error updating item: ${item.id()}")
                onError(e)
            }
        }
    }

    private fun update(item: FridgeItem, doUpdate: suspend (item: FridgeItem) -> Unit) {
        requireNotNull(viewModelScope).launch(context = Dispatchers.Default) {
            updateRunner.call(item, doUpdate, requireNotNull(handleError))
        }
    }

    internal fun consumeItem(item: FridgeItem) {
        update(item) { requireNotNull(interactor).commit(it.consume(currentDate())) }
    }

    internal fun restoreItem(item: FridgeItem) {
        update(item) {
            requireNotNull(interactor).commit(it.invalidateConsumption().invalidateSpoiled())
        }
    }

    internal fun spoilItem(item: FridgeItem) {
        update(item) { requireNotNull(interactor).commit(it.spoil(currentDate())) }
    }

    internal fun deleteItem(item: FridgeItem) {
        update(item) { requireNotNull(interactor).delete(it, true) }
    }

    internal fun updateItem(item: FridgeItem) {
        val updated = item.run {
            val dateOfPurchase = this.purchaseTime()
            if (this.presence() == FridgeItem.Presence.HAVE) {
                // If we are HAVE but don't have a purchase date, we just purchased the item!
                if (dateOfPurchase == null) {
                    val now = currentDate()
                    Timber.d("${item.name()} purchased! $now")
                    return@run this.purchaseTime(now)
                }
            } else {
                // If we are NEED but have a purchase date, we must invalidate the date
                if (dateOfPurchase != null) {
                    Timber.d("${item.name()} purchase date cleared")
                    return@run this.invalidatePurchase()
                }
            }

            // No side effects
            return@run this
        }

        update(
            updated,
            doUpdate = requireNotNull(interactor)::commit,
        )
    }
}

