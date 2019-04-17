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

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.popinnow.android.refresh.RefreshLatch
import com.popinnow.android.refresh.newRefreshLatch
import com.pyamsoft.fridge.detail.list.DetailListUiComponent.Callback
import com.pyamsoft.fridge.detail.list.DetailListViewModel.DetailState
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.arch.renderOnChange

internal abstract class DetailListUiComponentImpl protected constructor(
  private val list: DetailList,
  private val viewModel: DetailListViewModel
) : BaseUiComponent<Callback>(),
  DetailListUiComponent {

  private lateinit var refreshLatch: RefreshLatch

  override fun id(): Int {
    return list.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      list.teardown()
      viewModel.unbind()
    }

    refreshLatch = newRefreshLatch(owner) { refreshing ->
      if (refreshing) {
        list.beginRefresh()
      } else {
        list.finishRefresh()
      }
    }

    list.inflate(savedInstanceState)
    viewModel.bind { state, oldState ->
      renderLoading(state, oldState)
      renderList(state, oldState)
      renderError(state, oldState)
    }
  }

  override fun onSaveState(outState: Bundle) {
    list.saveState(outState)
  }

  private fun renderLoading(state: DetailState, oldState: DetailState?) {
    state.renderOnChange(oldState, value = { it.isLoading }) { loading ->
      if (loading != null) {
        refreshLatch.isRefreshing = loading.isLoading
      }
    }
  }

  private fun renderList(state: DetailState, oldState: DetailState?) {
    state.renderOnChange(oldState, value = { it.items }) { items ->
      if (items.isEmpty()) {
        list.clearList()
      } else {
        list.setList(items)
      }
    }
  }

  private fun renderError(state: DetailState, oldState: DetailState?) {
    state.renderOnChange(oldState, value = { it.throwable }) { throwable ->
      if (throwable == null) {
        list.clearError()
      } else {
        list.showError(throwable)
      }
    }
  }

  override fun showError(throwable: Throwable) {
    list.showError(throwable)
  }
}
