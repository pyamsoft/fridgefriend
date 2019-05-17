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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.detail.list.DetailListControllerEvent.DatePick
import com.pyamsoft.fridge.detail.list.DetailListControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.list.DetailListViewEvent.PickDate
import com.pyamsoft.fridge.detail.list.DetailListViewState.Loading
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

class DetailListViewModel @Inject internal constructor(
  @Named("detail_entry_id") private val entryId: String,
  private val interactor: DetailListInteractor,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>
) : UiViewModel<DetailListViewState, DetailListViewEvent, DetailListControllerEvent>(
    initialState = DetailListViewState(
        isLoading = null,
        throwable = null,
        items = emptyList(),
        filterArchived = true
    )
) {

  private var refreshDisposable by singleDisposable()

  private var realtimeDisposable by singleDisposable()
  private var fakeRealtimeDisposable by singleDisposable()

  override fun handleViewEvent(event: DetailListViewEvent) {
    return when (event) {
      is ForceRefresh -> refreshList(true)
      is ExpandItem -> publish(ExpandForEditing(event.item))
      is PickDate -> publish(DatePick(event.oldItem, event.year, event.month, event.day))
    }
  }

  override fun onCleared() {
    realtimeDisposable.tryDispose()
    fakeRealtimeDisposable.tryDispose()
  }

  fun fetchItems() {
    refreshList(false)
  }

  @CheckResult
  private fun getItems(force: Boolean): Single<List<FridgeItem>> {
    return interactor.getItems(entryId, force)
  }

  @CheckResult
  private fun listenForChanges(): Observable<FridgeItemChangeEvent> {
    return interactor.listenForChanges(entryId)
  }

  @CheckResult
  private fun getListItems(items: List<FridgeItem>): List<FridgeItem> {
    val mutableItems = items.toMutableList()
    insert(mutableItems, FridgeItem.empty(entryId))
    return mutableItems.sortedWith(Comparator { o1, o2 ->
      return@Comparator when {
        o1.id().isBlank() -> 1
        o2.id().isBlank() -> -1
        else -> 0
      }
    })
  }

  private fun refreshList(force: Boolean) {
    refreshDisposable = getItems(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { refreshDisposable.tryDispose() }
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

  private fun insert(
    items: MutableList<FridgeItem>,
    item: FridgeItem
  ) {
    if (!checkExists(items, item)) {
      addToEndBeforeAddNew(items, item)
    } else {
      for ((index, oldItem) in items.filterNot {
        it.id()
            .isBlank()
      }.withIndex()) {
        if (oldItem.id() == item.id()) {
          items[index] = item
          break
        }
      }
    }
  }

  private fun addToEndBeforeAddNew(
    items: MutableList<FridgeItem>,
    item: FridgeItem
  ) {
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
  private fun checkExists(
    items: MutableList<FridgeItem>,
    item: FridgeItem
  ): Boolean {
    return items.any { item.id() == it.id() && item.entryId() == it.entryId() }
  }

  private fun handleRealtimeInsert(item: FridgeItem) {
    setState {
      copy(
          items = items.let { list ->
            val newItems = list.toMutableList()
            insert(newItems, item)
            return@let getListItems(newItems)
                .filterNot { filterArchived && it.isArchived() }
          })
    }
  }

  private fun handleRealtimeUpdate(item: FridgeItem) {
    // Remove Archived items
    if (item.isArchived()) {
      setState {
        copy(
            items = getListItems(items.map { old ->
              if (old.id() == item.id()) {
                return@map item
              } else {
                return@map old
              }
            }).filterNot { filterArchived && it.isArchived() }
        )
      }
    } else {
      setState {
        copy(
            items = getListItems(items.map { old ->
              if (old.id() == item.id()) {
                return@map item
              } else {
                return@map old
              }
            }).filterNot { filterArchived && it.isArchived() }
        )
      }

    }
  }

  private fun handleRealtimeDelete(item: FridgeItem) {
    setState {
      copy(
          items = getListItems(items.filterNot { it.id() == item.id() })
              .filterNot { filterArchived && it.isArchived() }
      )
    }
  }

  private fun handleListRefreshBegin() {
    setState {
      copy(isLoading = Loading(true))
    }
  }

  private fun handleListRefreshed(items: List<FridgeItem>) {
    Timber.d("Items list: $items")
    setState {
      copy(
          items = getListItems(items.filterNot { filterArchived && it.isArchived() }),
          throwable = null
      )
    }
  }

  private fun handleListRefreshError(throwable: Throwable) {
    setState {
      copy(items = emptyList(), throwable = throwable)
    }
  }

  private fun handleListRefreshComplete() {
    setState {
      copy(isLoading = Loading(false))
    }
  }

}
