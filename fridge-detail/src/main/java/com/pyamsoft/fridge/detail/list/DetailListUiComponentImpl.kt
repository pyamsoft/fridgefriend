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
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.list.DetailListUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import javax.inject.Inject

internal class DetailListUiComponentImpl @Inject internal constructor(
  private val list: DetailList,
  private val presenter: DetailListPresenter
) : BaseUiComponent<DetailListUiComponent.Callback>(),
  DetailListUiComponent,
  DetailListPresenter.Callback {

  override fun id(): Int {
    return list.id()
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      list.teardown()
      presenter.unbind()
    }

    list.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    list.saveState(outState)
  }

  override fun handleListRefreshBegin() {
    list.beginRefresh()
  }

  override fun handleListRefreshed(items: List<FridgeItem>) {
    list.setList(items)
  }

  override fun handleListRefreshError(throwable: Throwable) {
    list.showError(throwable)
  }

  override fun handleListRefreshComplete() {
    list.finishRefresh()
  }

  override fun handleRealtimeInsert(item: FridgeItem) {
    list.insert(item)
  }

  override fun handleRealtimeUpdate(item: FridgeItem) {
    list.update(item)
  }

  override fun handleRealtimeDelete(item: FridgeItem) {
    list.delete(item)
  }

  override fun showError(throwable: Throwable) {
    list.showError(throwable)
  }
}
