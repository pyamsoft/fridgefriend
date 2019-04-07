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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.list.EntryListPresenter.EntryState
import com.pyamsoft.fridge.entry.list.EntryListUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import javax.inject.Inject

internal class EntryListUiComponentImpl @Inject internal constructor(
  private val listView: EntryList,
  private val presenter: EntryListPresenter
) : BaseUiComponent<EntryListUiComponent.Callback>(),
  EntryListUiComponent,
  EntryListPresenter.Callback {

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      listView.teardown()
      presenter.unbind()
    }

    listView.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    listView.saveState(outState)
  }

  override fun onRender(state: EntryState, oldState: EntryState?) {
    renderLoading(state, oldState)
    renderList(state, oldState)
    renderError(state, oldState)
  }

  private fun renderLoading(state: EntryState, oldState: EntryState?) {
    state.isLoading.let { loading ->
      if (oldState == null || loading != oldState.isLoading) {
        if (loading) {
          listView.beginRefresh()
        } else {
          listView.finishRefresh()
        }
      }
    }
  }

  private fun renderList(state: EntryState, oldState: EntryState?) {
    state.entries.let { entries ->
      if (oldState == null || entries != oldState.entries) {
        if (entries.isEmpty()) {
          listView.clearList()
        } else {
          listView.setList(entries)
        }
      }
    }
  }

  private fun renderError(state: EntryState, oldState: EntryState?) {
    state.throwable.let { throwable ->
      if (oldState == null || throwable != oldState.throwable) {
        if (throwable == null) {
          listView.clearError()
        } else {
          listView.showError(throwable)
        }
      }
    }
  }

  override fun handleEditEntry(entry: FridgeEntry) {
    callback.onEditEntry(entry.id())
  }

}
