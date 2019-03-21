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

package com.pyamsoft.fridge.entry.list

import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.DeleteGroup
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.InsertGroup
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.UpdateGroup
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryScope
import com.pyamsoft.fridge.entry.list.EntryListPresenter.Callback
import com.pyamsoft.pydroid.arch.BasePresenter
import com.pyamsoft.pydroid.core.bus.RxBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

@EntryScope
internal class EntryListPresenter @Inject internal constructor(
  private val interactor: EntryListInteractor
) : BasePresenter<Unit, Callback>(RxBus.empty()),
  EntryList.Callback {

  private var refreshDisposable by singleDisposable()
  private var realtimeChangeDisposable by singleDisposable()

  override fun onBind() {
    refresh(false)
  }

  override fun onUnbind() {
    refreshDisposable.tryDispose()
    realtimeChangeDisposable.tryDispose()
  }

  fun refresh(force: Boolean) {
    realtimeChangeDisposable.tryDispose()
    refreshDisposable = interactor.getEntries(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doOnSubscribe { callback.onListRefreshBegin() }
      .doAfterTerminate { callback.onListRefreshComplete() }
      .doAfterSuccess { beginListeningForChanges() }
      .subscribe({ callback.onListRefreshed(it) }, {
        Timber.e(it, "Error refreshing entry list")
        callback.onListRefreshError(it)
      })
  }

  private fun beginListeningForChanges() {
    realtimeChangeDisposable = interactor.listenForChanges()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        return@subscribe when (it) {
          is Insert -> callback.onRealtimeInsert(it.item)
          is InsertGroup -> callback.onRealtimeInsert(it.items)
          is Update -> callback.onRealtimeUpdate(it.item)
          is UpdateGroup -> callback.onRealtimeInsert(it.items)
          is Delete -> callback.onRealtimeDelete(it.id)
          is DeleteGroup -> callback.onRealtimeDelete(it.ids)
          is DeleteAll -> callback.onRealtimeDeleteAll()
        }
      }
  }

  interface Callback {

    fun onListRefreshBegin()

    fun onListRefreshed(data: List<FridgeItem>)

    fun onListRefreshError(throwable: Throwable)

    fun onListRefreshComplete()

    fun onRealtimeInsert(item: FridgeItem)

    fun onRealtimeInsert(items: List<FridgeItem>)

    fun onRealtimeUpdate(item: FridgeItem)

    fun onRealtimeUpdate(items: List<FridgeItem>)

    fun onRealtimeDelete(id: String)

    fun onRealtimeDelete(ids: List<String>)

    fun onRealtimeDeleteAll()

  }

}
