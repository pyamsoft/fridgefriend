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

import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarControllerEvent.EntryArchived
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarViewEvent.Archive
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarViewEvent.Close
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class CreationToolbarViewModel @Inject internal constructor(
  private val interactor: CreationToolbarInteractor
) : UiViewModel<CreationToolbarViewState, CreationToolbarViewEvent, CreationToolbarControllerEvent>(
    initialState = CreationToolbarViewState(isReal = false, throwable = null)
) {

  private var observeRealDisposable by singleDisposable()
  private var observeDeleteDisposable by singleDisposable()

  private var deleteDisposable by singleDisposable()

  override fun handleViewEvent(event: CreationToolbarViewEvent) {
    return when (event) {
      is Archive -> handleArchived()
      is Close -> publish(NavigateUp)
    }
  }

  override fun onCleared() {
    observeRealDisposable.tryDispose()
    observeDeleteDisposable.tryDispose()
  }

  fun beginMonitoringEntry() {
    observeReal(false)
    listenForDelete()
  }

  private fun observeReal(force: Boolean) {
    observeRealDisposable = interactor.observeEntryReal(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ handleRealUpdated(it) }, {
          Timber.e(it, "Error observing entry real")
          handleError(it)
        })
  }

  private fun handleRealUpdated(real: Boolean) {
    setState { copy(isReal = real) }
  }

  private fun listenForDelete() {
    observeDeleteDisposable = interactor.listenForArchived()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { publish(EntryArchived) }
  }

  private fun handleArchived() {
    deleteDisposable = interactor.archive()
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

}
