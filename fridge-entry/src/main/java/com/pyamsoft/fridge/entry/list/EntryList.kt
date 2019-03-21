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

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pyamsoft.fridge.db.FridgeItem
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.fridge.entry.list.EntryList.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.refreshing
import javax.inject.Inject

internal class EntryList @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.entry_list

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.entry_swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.entry_list)
  private val emptyState by lazyView<TextView>(R.id.entry_empty)

  fun beginRefresh() {
    layoutRoot.refreshing(true)
    // TODO clear list
  }

  fun setList(items: List<FridgeItem>) {
    // TODO add items to list
  }

  fun showError(throwable: Throwable) {
    // TODO clear list
    // TODO set error text
  }

  fun finishRefresh() {
    // TODO Decide view state based on number of list items
  }

  interface Callback {

  }
}
