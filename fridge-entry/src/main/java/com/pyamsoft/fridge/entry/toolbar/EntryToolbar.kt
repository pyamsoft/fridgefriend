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

package com.pyamsoft.fridge.entry.toolbar

import android.os.Bundle
import android.view.MenuItem
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject
import javax.inject.Named

internal class EntryToolbar @Inject internal constructor(
  private val toolbarActivity: ToolbarActivity,
  @Named("app_name") private val appNameRes: Int,
  private val callback: Callback
) : UiView {

  override fun id(): Int {
    throw InvalidIdException
  }

  private var settingsItem: MenuItem? = null

  override fun inflate(savedInstanceState: Bundle?) {
    setupToolbar()
  }

  override fun saveState(outState: Bundle) {
  }

  override fun teardown() {
    hideMenu()
  }

  private fun setupToolbar() {
    toolbarActivity.requireToolbar { toolbar ->
      toolbar.setUpEnabled(false)
      toolbar.setTitle(appNameRes)
    }

    inflateMenu()
  }

  private fun hideMenu() {
    settingsItem?.setOnMenuItemClickListener(null)
    settingsItem = null
    toolbarActivity.withToolbar { it.menu.removeItem(R.id.menu_item_settings) }
  }

  private fun inflateMenu() {
    toolbarActivity.requireToolbar { toolbar ->
      if (toolbar.menu.findItem(R.id.menu_item_settings) == null) {
        toolbar.inflateMenu(R.menu.toolbar_menu)
        toolbar.menu.findItem(R.id.menu_item_settings).also {
          it.setOnMenuItemClickListener {
            callback.onSettingsClicked()
            return@setOnMenuItemClickListener true
          }
          settingsItem = it
        }
      }
    }
  }

  fun showMenu(show: Boolean) {
    if (show) {
      setupToolbar()
    } else {
      hideMenu()
    }
  }

  interface Callback {

    fun onSettingsClicked()

  }

}
