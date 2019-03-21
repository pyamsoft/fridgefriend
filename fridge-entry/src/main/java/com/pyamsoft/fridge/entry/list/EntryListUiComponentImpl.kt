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
import com.pyamsoft.fridge.db.item.FridgeItem
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

  override fun onListRefreshBegin() {
    listView.beginRefresh()
  }

  override fun onListRefreshed(data: List<FridgeItem>) {
    listView.setList(data)
  }

  override fun onListRefreshError(throwable: Throwable) {
    listView.showError(throwable)
  }

  override fun onListRefreshComplete() {
    listView.finishRefresh()
  }

  override fun onRealtimeInsert(item: FridgeItem) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeInsert(items: List<FridgeItem>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeUpdate(item: FridgeItem) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeUpdate(items: List<FridgeItem>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeDelete(id: String) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeDelete(ids: List<String>) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun onRealtimeDeleteAll() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}
