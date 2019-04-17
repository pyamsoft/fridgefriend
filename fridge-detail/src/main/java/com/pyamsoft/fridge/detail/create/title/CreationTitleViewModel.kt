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

package com.pyamsoft.fridge.detail.create.title

import com.pyamsoft.fridge.detail.DetailConstants
import com.pyamsoft.fridge.detail.create.title.CreationTitleHandler.TitleEvent
import com.pyamsoft.fridge.detail.create.title.CreationTitleViewModel.NameState
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.arch.UiState
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

internal class CreationTitleViewModel @Inject internal constructor(
  private val handler: UiEventHandler<TitleEvent, CreationTitle.Callback>,
  private val interactor: CreationTitleInteractor
) : UiViewModel<NameState>(
  initialState = NameState(name = "", throwable = null)
), CreationTitle.Callback {

  private var updateDisposable by singleDisposable()

  override fun onBind() {
    handler.handle(this).destroy()

    observeName(false)
  }

  override fun onUnbind() {
    // Don't dispose updateDisposable here as it may need to outlive the View
    // since the final commit happens as the View is tearing down
  }

  private fun observeName(force: Boolean) {
    interactor.observeEntryName(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({ handleNameUpdated(it.name) }, {
        Timber.e(it, "Error observing entry name")
        handleNameUpdateError(it)
      }).destroy()
  }

  private fun handleNameUpdated(name: String) {
    setState { copy(name = name, throwable = null) }
  }

  override fun onUpdateName(name: String) {
    updateDisposable = Completable.complete()
      .delay(DetailConstants.COMMIT_TIMEOUT_DURATION, DetailConstants.COMMIT_TIMEOUT_UNIT)
      .andThen(interactor.saveName(name.trim()))
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate {
        // Dispose ourselves here after we are done
        updateDisposable.tryDispose()
      }
      .subscribe({ }, {
        Timber.e(it, "Error updating entry name")
        handleNameUpdateError(it)
      })
  }

  private fun handleNameUpdateError(throwable: Throwable) {
    setState { copy(name = "", throwable = throwable) }
  }

  data class NameState(val name: String, val throwable: Throwable?) : UiState

}
