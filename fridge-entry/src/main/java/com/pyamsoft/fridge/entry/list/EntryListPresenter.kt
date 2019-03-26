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

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Delete
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.DeleteAll
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Insert
import com.pyamsoft.fridge.db.entry.FridgeEntryChangeEvent.Update
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

  override fun onRefresh() {
    refresh(true)
  }

  private fun refresh(force: Boolean) {
    realtimeChangeDisposable.tryDispose()
    refreshDisposable = interactor.getEntries(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doOnSubscribe { callback.handleListRefreshBegin() }
      .doAfterTerminate { callback.handleListRefreshComplete() }
      .doAfterSuccess { beginListeningForChanges() }
      .subscribe({ callback.handleListRefreshed(it) }, {
        Timber.e(it, "Error refreshing entry list")
        callback.handleListRefreshError(it)
      })
  }

  private fun beginListeningForChanges() {
    realtimeChangeDisposable = interactor.listenForChanges()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        return@subscribe when (it) {
          is Insert -> callback.handleRealtimeInsert(it.entry)
          is Update -> callback.handleRealtimeUpdate(it.entry)
          is Delete -> callback.handleRealtimeDelete(it.entry)
          is DeleteAll -> callback.handleRealtimeDeleteAll()
        }
      }
  }

  override fun onItemClicked(entry: FridgeEntry) {
    Timber.d("Edit entry: $entry")
    callback.handleEditEntry(entry)
  }

  interface Callback {

    fun handleEditEntry(entry: FridgeEntry)

    fun handleListRefreshBegin()

    fun handleListRefreshed(data: List<FridgeEntry>)

    fun handleListRefreshError(throwable: Throwable)

    fun handleListRefreshComplete()

    fun handleRealtimeInsert(entry: FridgeEntry)

    fun handleRealtimeUpdate(entry: FridgeEntry)

    fun handleRealtimeDelete(entry: FridgeEntry)

    fun handleRealtimeDeleteAll()

  }

}
