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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@DetailScope
internal class DetailTitlePresenter @Inject internal constructor(
  private val interactor: DetailTitleInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
  DetailTitle.Callback {

  private var observeNameDisposable by singleDisposable()
  private var updateNameDisposable by singleDisposable()

  override fun onBind() {
    observeName(false)
  }

  override fun onUnbind() {
    observeNameDisposable.tryDispose()
    updateNameDisposable.tryDispose()
  }

  fun observeName(force: Boolean) {
    observeNameDisposable = interactor.observeEntryName(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({ callback.handleNameUpdated(it.name, it.firstUpdate) }, {
        Timber.e(it, "Error observing entry name")
        callback.handleNameUpdateError(it)
      })
  }

  override fun onUpdateName(name: String) {
    updateNameDisposable = interactor.saveName(name)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({ Timber.d("Entry name updated: $name") }, {
        Timber.e(it, "Error updating entry name")
        callback.handleNameUpdateError(it)
      })
  }

  interface Callback {

    fun handleNameUpdated(name: String, firstUpdate: Boolean)

    fun handleNameUpdateError(throwable: Throwable)

  }

}
