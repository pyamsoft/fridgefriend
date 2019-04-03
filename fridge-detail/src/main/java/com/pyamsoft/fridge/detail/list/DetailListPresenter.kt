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

package com.pyamsoft.fridge.detail.list

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.detail.DetailScope
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@DetailScope
internal class DetailListPresenter @Inject internal constructor(
  private val interactor: DetailListInteractor
) : BasePresenter<Unit, DetailListPresenter.Callback>(RxBus.empty()),
  DetailList.Callback {

  private var refreshDisposable by singleDisposable()
  private var realtimeDisposable by singleDisposable()

  override fun onBind() {
    refreshList(false)
  }

  override fun onUnbind() {
    refreshDisposable.tryDispose()
    realtimeDisposable.tryDispose()
  }

  override fun onRefresh() {
    refreshList(true)
  }

  private fun refreshList(force: Boolean) {
    realtimeDisposable.tryDispose()
    refreshDisposable = interactor.getItems(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { callback.handleListRefreshComplete() }
      .doOnSubscribe { callback.handleListRefreshBegin() }
      .doAfterSuccess { beginListeningForChanges() }
      .subscribe({ callback.handleListRefreshed(it) }, {
        Timber.e(it, "Error refreshing item list")
        callback.handleListRefreshError(it)
      })
  }

  private fun beginListeningForChanges() {
    realtimeDisposable = interactor.listenForChanges()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        return@subscribe when (it) {
          is Insert -> callback.handleRealtimeInsert(it.item)
          is Update -> callback.handleRealtimeUpdate(it.item)
          is Delete -> callback.handleRealtimeDelete(it.item)
        }
      }
  }

  interface Callback {

    fun handleListRefreshBegin()

    fun handleListRefreshed(items: List<FridgeItem>)

    fun handleListRefreshError(throwable: Throwable)

    fun handleListRefreshComplete()

    fun handleRealtimeInsert(item: FridgeItem)

    fun handleRealtimeUpdate(item: FridgeItem)

    fun handleRealtimeDelete(item: FridgeItem)

  }

}
