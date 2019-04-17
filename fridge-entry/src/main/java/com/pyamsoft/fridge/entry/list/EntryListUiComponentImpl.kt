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

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.popinnow.android.refresh.RefreshLatch
import com.popinnow.android.refresh.newRefreshLatch
import com.pyamsoft.fridge.entry.list.EntryListUiComponent.Callback
import com.pyamsoft.fridge.entry.list.EntryListViewModel.EntryState
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.arch.renderOnChange
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import javax.inject.Inject

internal class EntryListUiComponentImpl @Inject internal constructor(
  private val listView: EntryList,
  private val viewModel: EntryListViewModel
) : BaseUiComponent<Callback>(),
  EntryListUiComponent {

  private lateinit var refreshLatch: RefreshLatch

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      listView.teardown()
      viewModel.unbind()
    }

    refreshLatch = newRefreshLatch(owner) { refreshing ->
      if (refreshing) {
        listView.beginRefresh()
      } else {
        listView.finishRefresh()
      }
    }

    listView.inflate(savedInstanceState)
    viewModel.bind { state, oldState ->
      renderLoading(state, oldState)
      renderList(state, oldState)
      renderError(state, oldState)
      renderEdit(state, oldState)
    }
  }

  override fun onSaveState(outState: Bundle) {
    listView.saveState(outState)
  }

  private fun renderLoading(state: EntryState, oldState: EntryState?) {
    state.renderOnChange(oldState, value = { it.isLoading }) { loading ->
      if (loading != null) {
        refreshLatch.isRefreshing = loading.isLoading
      }
    }
  }

  private fun renderList(state: EntryState, oldState: EntryState?) {
    state.renderOnChange(oldState, value = { it.entries }) { entries ->
      if (entries.isEmpty()) {
        listView.clearList()
      } else {
        listView.setList(entries)
      }
    }
  }

  private fun renderError(state: EntryState, oldState: EntryState?) {
    state.renderOnChange(oldState, value = { it.throwable }) { throwable ->
      if (throwable == null) {
        listView.clearError()
      } else {
        listView.showError(throwable)
      }
    }
  }

  private fun renderEdit(state: EntryState, oldState: EntryState?) {
    state.renderOnChange(oldState, value = { it.editingEntry }) { editingEntry ->
      if (editingEntry != null) {
        callback.onEditEntry(editingEntry.id())
      }
    }
  }

}
