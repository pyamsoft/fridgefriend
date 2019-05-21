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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Update
import com.pyamsoft.fridge.detail.DetailControllerEvent.DatePick
import com.pyamsoft.fridge.detail.DetailControllerEvent.EntryArchived
import com.pyamsoft.fridge.detail.DetailControllerEvent.ExpandForEditing
import com.pyamsoft.fridge.detail.DetailControllerEvent.NavigateUp
import com.pyamsoft.fridge.detail.DetailViewEvent.ArchiveEntry
import com.pyamsoft.fridge.detail.DetailViewEvent.CloseEntry
import com.pyamsoft.fridge.detail.DetailViewEvent.ExpandItem
import com.pyamsoft.fridge.detail.DetailViewEvent.ForceRefresh
import com.pyamsoft.fridge.detail.DetailViewEvent.NameUpdate
import com.pyamsoft.fridge.detail.DetailViewEvent.PickDate
import com.pyamsoft.fridge.detail.DetailViewEvent.ToggleArchiveVisibility
import com.pyamsoft.fridge.detail.DetailViewState.Loading
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class DetailViewModel @Inject internal constructor(
  private val interactor: DetailInteractor,
  private val fakeRealtime: EventBus<FridgeItemChangeEvent>,
  private val listItemPresence: FridgeItem.Presence,
  entry: FridgeEntry
) : UiViewModel<DetailViewState, DetailViewEvent, DetailControllerEvent>(
    initialState = DetailViewState(
        entry = entry,
        isLoading = null,
        throwable = null,
        items = emptyList(),
        filterArchived = true
    )
) {

  private val entryId = entry.id()

  private var deleteDisposable by singleDisposable()
  private var refreshDisposable by singleDisposable()
  private var nameDisposable by singleDisposable()

  private var observeRealDisposable by singleDisposable()
  private var observeDeleteDisposable by singleDisposable()
  private var realtimeDisposable by singleDisposable()
  private var fakeRealtimeDisposable by singleDisposable()

  override fun handleViewEvent(event: DetailViewEvent) {
    return when (event) {
      is ForceRefresh -> refreshList(true)
      is ExpandItem -> publish(ExpandForEditing(event.item))
      is PickDate -> publish(DatePick(event.oldItem, event.year, event.month, event.day))
      is ArchiveEntry -> handleArchived()
      is CloseEntry -> publish(NavigateUp)
      is NameUpdate -> updateName(event.name)
      is ToggleArchiveVisibility -> toggleArchived(event.show)
    }
  }

  private fun toggleArchived(show: Boolean) {
    setState { copy(filterArchived = !show) }
    refreshList(false)
  }

  override fun onCleared() {
    realtimeDisposable.tryDispose()
    fakeRealtimeDisposable.tryDispose()
  }

  fun fetchItems() {
    refreshList(false)
    observeReal(false)
    listenForDelete()
  }

  private fun updateName(name: String) {
    nameDisposable = Completable.complete()
        .andThen(interactor.saveName(name.trim()))
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { nameDisposable.tryDispose() }
        .subscribe({ }, {
          Timber.e(it, "Error updating entry name")
          handleNameUpdateError(it)
        })
  }

  private fun handleNameUpdateError(throwable: Throwable) {
    setState { copy(throwable = throwable) }
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
  private fun getListItems(
    filterArchived: Boolean,
    items: List<FridgeItem>
  ): List<FridgeItem> {
    val mutableItems = items.toMutableList()
    insert(mutableItems, FridgeItem.empty(entryId))
    return mutableItems
        .asSequence()
        .sortedWith(Comparator { o1, o2 ->
          return@Comparator when {
            o1.id().isBlank() -> 1
            o2.id().isBlank() -> -1
            else -> {
              when {
                o1.isArchived() && o2.isArchived() -> 0
                o1.isArchived() -> 1
                o2.isArchived() -> -1
                else -> 0
              }
            }
          }
        })
        .filterNot { filterArchived && it.isArchived() }
        .filter {
          if (it.id().isBlank()) {
            // Add item
            return@filter true
          } else {
            return@filter it.presence() == listItemPresence
          }
        }
        .toList()
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
            return@let getListItems(filterArchived, newItems)
          })
    }
  }

  private fun handleRealtimeUpdate(item: FridgeItem) {
    // Remove Archived items
    if (item.isArchived()) {
      setState {
        copy(
            items = getListItems(filterArchived, items.filterNot { it.id() == item.id() })
        )
      }
    } else {
      setState {
        copy(
            items = getListItems(filterArchived,
                if (items.map { it.id() }.contains(item.id())) {
                  items.map { old ->
                    if (old.id() == item.id()) {
                      return@map item
                    } else {
                      return@map old
                    }
                  }
                } else {
                  items + item
                }
            )
        )
      }

    }
  }

  private fun handleRealtimeDelete(item: FridgeItem) {
    setState {
      copy(items = getListItems(filterArchived, items.filterNot { it.id() == item.id() }))
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
      copy(items = getListItems(filterArchived, items))
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

  private fun observeReal(force: Boolean) {
    observeRealDisposable = interactor.observeEntry(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ handleEntryUpdated(it) }, {
          Timber.e(it, "Error observing entry real")
          setState { copy(throwable = it) }
        })
  }

  private fun handleEntryUpdated(entry: FridgeEntry) {
    setState { copy(entry = entry) }
  }

  private fun listenForDelete() {
    observeDeleteDisposable = interactor.listenForEntryArchived()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { publish(EntryArchived) }
  }

  private fun handleArchived() {
    deleteDisposable = interactor.archiveEntry()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doAfterTerminate { deleteDisposable.tryDispose() }
        .subscribe({}, {
          Timber.e(it, "Error observing delete stream")
          setState { copy(throwable = it) }
        })
  }

}
