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
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntryRealtime
import com.pyamsoft.fridge.entry.list.EntryListControllerEvent.OpenForEditing
import com.pyamsoft.fridge.entry.list.EntryListViewEvent.ForceRefresh
import com.pyamsoft.fridge.entry.list.EntryListViewEvent.OpenEntry
import com.pyamsoft.fridge.entry.list.EntryListViewState.Loading
import com.pyamsoft.pydroid.arch.impl.BaseUiViewModel
import com.pyamsoft.pydroid.core.singleDisposable
import com.pyamsoft.pydroid.core.tryDispose
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject

class EntryListViewModel @Inject internal constructor(
  private val queryDao: FridgeEntryQueryDao,
  private val realtime: FridgeEntryRealtime
) : BaseUiViewModel<EntryListViewState, EntryListViewEvent, EntryListControllerEvent>(
    initialState = EntryListViewState(
        isLoading = null,
        throwable = null,
        entries = emptyList()
    )
) {

  private var refreshDisposable by singleDisposable()
  private var realtimeChangeDisposable by singleDisposable()

  override fun onBind() {
    refresh(false)
  }

  override fun handleViewEvent(event: EntryListViewEvent) {
    return when (event) {
      is ForceRefresh -> refresh(true)
      is OpenEntry -> openEntry(event.entry)
    }
  }

  override fun onUnbind() {
    realtimeChangeDisposable.tryDispose()
  }

  private fun refresh(force: Boolean) {
    refreshDisposable = queryDao.queryAll(force)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .doOnSubscribe { handleListRefreshBegin() }
        .doAfterTerminate { refreshDisposable.tryDispose() }
        .doAfterTerminate { handleListRefreshComplete() }
        .doAfterSuccess { beginListeningForChanges() }
        .subscribe({ handleListRefreshed(it) }, {
          Timber.e(it, "Error refreshing entry list")
          handleListRefreshError(it)
        })
  }

  private fun beginListeningForChanges() {
    realtimeChangeDisposable = realtime.listenForChanges()
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          return@subscribe when (it) {
            is Insert -> handleRealtimeInsert(it.entry)
            is Update -> handleRealtimeUpdate(it.entry)
            is Delete -> handleRealtimeDelete(it.entry)
            is DeleteAll -> handleRealtimeDeleteAll()
          }
        }
  }

  private fun handleRealtimeInsert(entry: FridgeEntry) {
    setState {
      copy(entries = (entries.toMutableList() + entry).filterNot { it.isArchived() })
    }
  }

  private fun handleRealtimeUpdate(entry: FridgeEntry) {
    setState {
      copy(entries = entries.map { old ->
        if (old.id() == entry.id()) {
          return@map entry
        } else {
          return@map old
        }
      }.filterNot { it.isArchived() })
    }
  }

  private fun handleRealtimeDelete(entry: FridgeEntry) {
    setState {
      copy(entries = entries.filterNot { it.id() == entry.id() }.filterNot { it.isArchived() })
    }
  }

  private fun handleRealtimeDeleteAll() {
    setState {
      copy(entries = emptyList())
    }
  }

  private fun handleListRefreshBegin() {
    setState {
      copy(isLoading = Loading(true))
    }
  }

  private fun handleListRefreshed(entries: List<FridgeEntry>) {
    Timber.d("List refreshed: $entries")
    setState {
      copy(entries = entries.filterNot { it.isArchived() }, throwable = null)
    }
  }

  private fun handleListRefreshError(throwable: Throwable) {
    setState {
      copy(entries = emptyList(), throwable = throwable)
    }
  }

  private fun handleListRefreshComplete() {
    setState {
      copy(isLoading = Loading(false))
    }
  }

  private fun openEntry(entry: FridgeEntry) {
    Timber.d("Edit entry: $entry")
    publish(OpenForEditing(entry))
  }

}
