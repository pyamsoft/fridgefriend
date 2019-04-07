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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.detail.DetailListPresenter.DetailState
import com.pyamsoft.pydroid.arch.Presenter
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber

internal abstract class DetailListPresenter protected constructor(
  protected val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : Presenter<DetailState, DetailListPresenter.Callback>(),
  DetailList.Callback {

  private var refreshDisposable by singleDisposable()
  private var realtimeDisposable by singleDisposable()
  private var fakeRealtimeDisposable by singleDisposable()

  final override fun initialState(): DetailState {
    return DetailState(isLoading = false, throwable = null, items = emptyList())
  }

  @CheckResult
  protected abstract fun getItems(force: Boolean): Single<List<FridgeItem>>

  @CheckResult
  protected abstract fun listenForChanges(): Observable<FridgeItemChangeEvent>

  @CheckResult
  protected abstract fun getListItems(items: List<FridgeItem>): List<FridgeItem>

  final override fun onBind() {
    refreshList(false)
  }

  final override fun onUnbind() {
    refreshDisposable.tryDispose()
    realtimeDisposable.tryDispose()
    fakeRealtimeDisposable.tryDispose()
  }

  final override fun onRefresh() {
    refreshList(true)
  }

  private fun refreshList(force: Boolean) {
    realtimeDisposable.tryDispose()
    refreshDisposable = getItems(force)
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .doAfterTerminate { handleListRefreshComplete() }
      .doOnSubscribe { handleListRefreshBegin() }
      .doAfterSuccess { beginListeningForChanges() }
      .subscribe({ handleListRefreshed(it) }, {
        Timber.e(it, "Error refreshing item list")
        handleListRefreshError(it)
      })
  }

  private fun handleRealtime(event: FridgeItemChangeEvent) {
    return when (event) {
      is Insert -> handleRealtimeInsert(event.item)
      is Update -> handleRealtimeUpdate(event.item)
      is Delete -> handleRealtimeDelete(event.item)
    }
  }

  private fun beginListeningForChanges() {
    realtimeDisposable = listenForChanges()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { handleRealtime(it) }

    fakeRealtimeDisposable = fakeRealtime.listen()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe { handleRealtime(it) }
  }

  protected fun insert(items: MutableList<FridgeItem>, item: FridgeItem) {
    if (!updateExistingItem(items, item)) {
      addToEndBeforeAddNew(items, item)
    }
  }

  private fun addToEndBeforeAddNew(items: MutableList<FridgeItem>, item: FridgeItem) {
    var index = -1
    for ((i, e) in items.withIndex()) {
      if (e.id().isBlank()) {
        index = i
        break
      }
    }

    when {
      index == 0 -> items.add(0, item)
      index > 0 -> items.add(index, item)
      else -> items.add(item)
    }
  }

  @CheckResult
  private fun updateExistingItem(items: MutableList<FridgeItem>, item: FridgeItem): Boolean {
    for ((index, e) in items.withIndex()) {
      if (item.id() == e.id() && item.entryId() == e.entryId()) {
        items[index] = item
        return true
      }
    }

    return false
  }

  private fun handleRealtimeInsert(item: FridgeItem) {
    setState {
      copy(items = items.let { list ->
        val newItems = list.toMutableList()
        insert(newItems, item)
        return@let getListItems(newItems)
      })
    }
  }

  private fun handleRealtimeUpdate(item: FridgeItem) {
    setState {
      copy(items = getListItems(items.map { old ->
        if (old.id() == item.id()) {
          return@map item
        } else {
          return@map old
        }
      }))
    }
  }

  private fun handleRealtimeDelete(item: FridgeItem) {
    setState {
      copy(items = getListItems(items.filterNot { it.id() == item.id() }))
    }
  }

  private fun handleListRefreshBegin() {
    setState {
      copy(isLoading = true)
    }
  }

  private fun handleListRefreshed(items: List<FridgeItem>) {
    setState {
      copy(items = getListItems(items), throwable = null)
    }
  }

  private fun handleListRefreshError(throwable: Throwable) {
    setState {
      copy(items = emptyList(), throwable = throwable)
    }
  }

  private fun handleListRefreshComplete() {
    setState {
      copy(isLoading = false)
    }
  }

  data class DetailState(
    val isLoading: Boolean,
    val throwable: Throwable?,
    val items: List<FridgeItem>
  )

  interface Callback : Presenter.Callback<DetailState>

}
