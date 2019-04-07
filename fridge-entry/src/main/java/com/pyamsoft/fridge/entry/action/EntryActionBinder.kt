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

import com.pyamsoft.fridge.entry.EntryScope
import com.pyamsoft.pydroid.arch.UiBinder
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@EntryScope
internal class EntryActionBinder @Inject internal constructor(
  private val interactor: EntryActionInteractor
) : UiBinder<EntryActionBinder.Callback>(),
  EntryCreate.Callback,
  EntryShop.Callback {

  private var createDisposable by singleDisposable()

  override fun onBind() {
  }

  override fun onUnbind() {
    createDisposable.tryDispose()
  }

  override fun onCreateClicked() {
    createDisposable = interactor.create()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe({
        callback.handleCreate(it.id())
      }, {
        Timber.e(it, "Error creating new entry")
        callback.handleCreateError(it)
      })
  }

  override fun onShopClicked() {
    callback.handleShop()
  }

  interface Callback : UiBinder.Callback {

    fun handleCreateError(throwable: Throwable)

    fun handleCreate(id: String)

    fun handleShop()

  }

}
