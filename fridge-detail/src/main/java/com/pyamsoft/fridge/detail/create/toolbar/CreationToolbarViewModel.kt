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

package com.pyamsoft.fridge.detail.create.toolbar

import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarViewModel.ToolbarState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class CreationToolbarViewModel @Inject internal constructor(
  private val handler: UiEventHandler<ToolbarEvent, CreationToolbar.Callback>,
  private val interactor: CreationToolbarInteractor
) : UiViewModel<ToolbarState>(
  initialState = ToolbarState(isReal = false, throwable = null, isBack = false, isDelete = false)
), CreationToolbar.Callback {

  private var deleteDisposable by singleDisposable()

  override fun onBind() {
    handler.handle(this).destroy()
    observeReal(false)
    listenForDelete()
  }

  override fun onUnbind() {
    // Do not dispose delete disposable here, we want it to finish
  }

  private fun observeReal(force: Boolean) {
    interactor.observeEntryReal(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({ handleRealUpdated(it) }, {
        Timber.e(it, "Error observing entry real")
        handleError(it)
      }).destroy()
  }

  private fun handleRealUpdated(real: Boolean) {
    setState { copy(isReal = real) }
  }

  private fun listenForDelete() {
    interactor.listenForDeleted()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { handleDeleted() }
      .destroy()
  }

  private fun handleDeleted() {
    setState { copy(isDelete = true) }
  }

  override fun onNavigationClicked() {
    handleBack()
  }

  private fun handleBack() {
    setState { copy(isBack = true) }
  }

  override fun onDeleteClicked() {
    deleteDisposable = interactor.delete()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { deleteDisposable.tryDispose() }
      .subscribe({}, {
        Timber.e(it, "Error observing delete stream")
        handleError(it)
      })
  }

  private fun handleError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
  }

  data class ToolbarState(
    val isReal: Boolean,
    val throwable: Throwable?,
    val isBack: Boolean,
    val isDelete: Boolean
  ) : UiState

}
