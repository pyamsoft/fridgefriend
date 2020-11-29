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
        onError: (Throwable) -> Unit
    ): UpdateDelegate {
        return UpdateDelegate(viewModelScope, interactor, onError)
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
                    (throwable: Throwable) -> Unit,
                    (item: FridgeItem) -> Unit
                > { item, doUpdate, onError, afterUpdate ->
            try {
                doUpdate(item.makeReal())
            } catch (error: Throwable) {
                error.onActualError { e ->
                    Timber.e(e, "Error updating item: ${item.id()}")
                    onError(e)
                }
            } finally {
                afterUpdate(item)
            }
        }

        private fun update(
            item: FridgeItem,
            doUpdate: suspend (item: FridgeItem) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit
        ) {
            requireNotNull(viewModelScope).launch(context = Dispatchers.Default) {
                updateRunner.call(item, doUpdate, requireNotNull(handleError), afterUpdate)
            }
        }

        internal fun consumeItem(
            item: FridgeItem,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { requireNotNull(interactor).commit(it.consume(currentDate())) },
                afterUpdate = afterUpdate
            )
        }

        internal fun restoreItem(
            item: FridgeItem,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = {
                    requireNotNull(interactor).commit(
                        it.invalidateConsumption().invalidateSpoiled()
                    )
                },
                afterUpdate = afterUpdate
            )
        }

        internal fun spoilItem(
            item: FridgeItem,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { requireNotNull(interactor).commit(it.spoil(currentDate())) },
                afterUpdate = afterUpdate
            )
        }

        internal fun deleteItem(
            item: FridgeItem,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = requireNotNull(interactor)::delete,
                afterUpdate = afterUpdate
            )
        }

        internal fun updateItem(item: FridgeItem) {
            update(
                item,
                doUpdate = requireNotNull(interactor)::commit,
                afterUpdate = EMPTY_DONE
            )
        }

        companion object {
            private val EMPTY_DONE: (FridgeItem) -> Unit = {}
        }
    }

}

