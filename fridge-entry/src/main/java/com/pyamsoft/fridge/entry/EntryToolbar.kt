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
import android.view.MenuItem
import com.pyamsoft.fridge.entry.EntryViewEvent.SettingsNavigate
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject
import javax.inject.Named

class EntryToolbar @Inject internal constructor(
  private val toolbarActivity: ToolbarActivity,
  @Named("app_name") private val appNameRes: Int
) : UiView<EntryViewState, EntryViewEvent>() {

  override fun id(): Int {
    throw InvalidIdException
  }

  private var settingsItem: MenuItem? = null

  override fun inflate(savedInstanceState: Bundle?) {
  }

  override fun saveState(outState: Bundle) {
  }

  override fun teardown() {
    teardownMenu()
  }

  private fun teardownMenu() {
    settingsItem?.setOnMenuItemClickListener(null)
    settingsItem = null
    toolbarActivity.withToolbar { it.menu.removeItem(R.id.menu_item_settings) }
  }

  private fun inflateMenu() {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.setUpEnabled(false)
      toolbar.setTitle(appNameRes)

      if (toolbar.menu.findItem(R.id.menu_item_settings) == null) {
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.menu.findItem(R.id.menu_item_settings)
            .also {
              it.setOnMenuItemClickListener {
                publish(SettingsNavigate)
                return@setOnMenuItemClickListener true
              }
              settingsItem = it
            }
      }
    }
  }

  private fun showMenu(show: Boolean) {
    if (show) {
      inflateMenu()
    } else {
      teardownMenu()
    }
  }

  override fun render(
    state: EntryViewState,
    savedState: UiSavedState
  ) {
    showMenu(state.isSettingsItemVisible)
  }

}
