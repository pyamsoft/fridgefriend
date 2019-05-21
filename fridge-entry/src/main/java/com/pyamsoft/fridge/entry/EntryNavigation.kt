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

package com.pyamsoft.fridge.entry

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenHave
import com.pyamsoft.fridge.entry.EntryViewEvent.OpenNeed
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class EntryNavigation @Inject internal constructor(
  parent: ViewGroup
) : BaseUiView<EntryViewState, EntryViewEvent>(parent) {

  override val layout: Int = R.layout.entry_navigation

  override val layoutRoot by boundView<ViewGroup>(R.id.entry_bottom_navigation)
  private val bottomNav by boundView<BottomNavigationView>(R.id.entry_bottom_navigation_menu)

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    bottomNav.isVisible = false
  }

  override fun onRender(
    state: EntryViewState,
    oldState: EntryViewState?
  ) {
    bottomNav.isVisible = state.entry != null

    bottomNav.setOnNavigationItemSelectedListener { item ->
      return@setOnNavigationItemSelectedListener when (item.itemId) {
        R.id.menu_item_nav_have -> handleClick(state.entry) { OpenHave(it) }
        R.id.menu_item_nav_need -> handleClick(state.entry) { OpenNeed(it) }
        else -> false
      }
    }
  }

  @CheckResult
  private fun handleClick(
    entry: FridgeEntry?,
    getEvent: (entry: FridgeEntry) -> EntryViewEvent
  ): Boolean {
    if (entry != null) {
      publish(getEvent(entry))
      return true
    } else {
      return false
    }
  }

  override fun onTeardown() {
    bottomNav.isVisible = false
    bottomNav.setOnNavigationItemSelectedListener(null)
  }

}
