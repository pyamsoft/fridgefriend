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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.fridge.entry.list.EntryList.Callback
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

internal class EntryList @Inject internal constructor(
  parent: ViewGroup,
  callback: Callback
) : BaseUiView<Callback>(parent, callback) {

  override val layout: Int = R.layout.swipe_refresh_list

  override val layoutRoot by lazyView<SwipeRefreshLayout>(R.id.swipe_refresh)

  private val recyclerView by lazyView<RecyclerView>(R.id.swipe_refresh_list)
  private val emptyState by lazyView<TextView>(R.id.swipe_refresh_empty)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    super.onInflated(view, savedInstanceState)
  }

  interface Callback {

  }
}
