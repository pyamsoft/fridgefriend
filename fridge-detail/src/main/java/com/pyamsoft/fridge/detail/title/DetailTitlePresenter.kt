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

package com.pyamsoft.fridge.detail.title

import com.pyamsoft.fridge.detail.DetailScope
import com.pyamsoft.fridge.detail.title.DetailTitlePresenter.Callback
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.util.concurrent.TimeUnit.SECONDS
import javax.inject.Inject

@DetailScope
internal class DetailTitlePresenter @Inject internal constructor(
  private val interactor: DetailTitleInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
  DetailTitle.Callback {

  private var updateDisposable by singleDisposable()

  override fun onBind() {
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
      .subscribe({ callback.handleNameUpdated(it.name, it.firstUpdate) }, {
        Timber.e(it, "Error observing entry name")
        callback.handleNameUpdateError(it)
      }).destroy()
  }

  override fun onUpdateName(name: String, finalUpdate: Boolean) {
    val source: Completable
    if (finalUpdate) {
      source = interactor.saveName(name, finalUpdate)
    } else {
      source = Completable.complete()
        .delay(1, SECONDS)
        .andThen(interactor.saveName(name, finalUpdate))
    }

    updateDisposable = source
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate {
        // Dispose ourselves here after we are done
        updateDisposable.tryDispose()
      }
      .subscribe({ }, {
        Timber.e(it, "Error updating entry name")
        callback.handleNameUpdateError(it)
      })
  }

  interface Callback {

    fun handleNameUpdated(name: String, firstUpdate: Boolean)

    fun handleNameUpdateError(throwable: Throwable)

  }

}
