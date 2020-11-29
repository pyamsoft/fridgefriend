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
    internal fun createUpdateDelegate(interactor: DetailInteractor): UpdateDelegate {
        return UpdateDelegate(viewModelScope, interactor)
    }

    internal class UpdateDelegate internal constructor(
        private val viewModelScope: CoroutineScope,
        private val interactor: DetailInteractor
    ) {

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
            onError: (throwable: Throwable) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit
        ) {
            viewModelScope.launch(context = Dispatchers.Default) {
                updateRunner.call(item, doUpdate, onError, afterUpdate)
            }
        }

        internal fun consumeItem(
            item: FridgeItem,
            onError: (throwable: Throwable) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { interactor.commit(it.consume(currentDate())) },
                onError = onError,
                afterUpdate = afterUpdate
            )
        }

        internal fun restoreItem(
            item: FridgeItem,
            onError: (throwable: Throwable) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { interactor.commit(it.invalidateConsumption().invalidateSpoiled()) },
                onError = onError,
                afterUpdate = afterUpdate
            )
        }

        internal fun spoilItem(
            item: FridgeItem,
            onError: (throwable: Throwable) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { interactor.commit(it.spoil(currentDate())) },
                onError = onError,
                afterUpdate = afterUpdate
            )
        }

        internal fun deleteItem(
            item: FridgeItem,
            onError: (throwable: Throwable) -> Unit,
            afterUpdate: (item: FridgeItem) -> Unit = EMPTY_DONE
        ) {
            update(
                item,
                doUpdate = { interactor.delete(it) },
                onError = onError,
                afterUpdate = afterUpdate
            )
        }

        internal fun updateItem(
            item: FridgeItem,
            onError: (throwable: Throwable) -> Unit
        ) {
            update(
                item,
                doUpdate = { interactor.commit(it) },
                onError = onError,
                afterUpdate = EMPTY_DONE
            )
        }

        companion object {
            private val EMPTY_DONE: (FridgeItem) -> Unit = {}
        }
    }

}

