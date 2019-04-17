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

package com.pyamsoft.fridge.entry.action

import com.pyamsoft.fridge.entry.action.EntryActionCreateHandler.CreateEvent
import com.pyamsoft.fridge.entry.action.EntryActionShopHandler.ShopEvent
import com.pyamsoft.fridge.entry.action.EntryActionViewModel.ActionState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class EntryActionViewModel @Inject internal constructor(
  private val createHandler: UiEventHandler<CreateEvent, EntryCreate.Callback>,
  private val shopHandler: UiEventHandler<ShopEvent, EntryShop.Callback>,
  private val interactor: EntryActionInteractor
) : UiViewModel<ActionState>(
  initialState = ActionState(throwable = null, isShopping = false, isCreating = "")
), EntryCreate.Callback, EntryShop.Callback {

  private var createDisposable by singleDisposable()

  override fun onBind() {
    createHandler.handle(this).destroy()
    shopHandler.handle(this).destroy()
  }

  override fun onUnbind() {
    createDisposable.tryDispose()
  }

  override fun onCreateClicked() {
    createDisposable = interactor.create()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        handleCreate(it.id())
      }, {
        Timber.e(it, "Error creating new entry")
        handleCreateError(it)
      })
  }

  private fun handleCreate(id: String) {
    setState { copy(isCreating = id) }
  }

  private fun handleCreateError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }

  override fun onShopClicked() {
    handleShop()
  }

  private fun handleShop() {
    setUniqueState(true, old = { it.isShopping }) { state, value ->
      state.copy(isShopping = value)
    }
  }

  data class ActionState(
    val throwable: Throwable?,
    val isShopping: Boolean,
    val isCreating: String
  ) : UiState

}
