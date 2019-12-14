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

import android.view.MenuItem
import com.pyamsoft.fridge.entry.EntryViewEvent.SettingsNavigate
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class EntryToolbar @Inject internal constructor(
    private val toolbarActivity: ToolbarActivity
) : UiView<EntryViewState, EntryViewEvent>() {

    private var settingsItem: MenuItem? = null

    init {
        doOnInflate {
            inflateMenu()
        }

        doOnTeardown {
            teardownMenu()
        }
    }

    override fun id(): Int {
        throw InvalidIdException
    }

    private fun teardownMenu() {
        settingsItem?.setOnMenuItemClickListener(null)
        settingsItem = null
        toolbarActivity.withToolbar { it.menu.removeItem(R.id.menu_item_settings) }
    }

    private fun inflateMenu() {
        toolbarActivity.withToolbar { toolbar ->
            toolbar.setUpEnabled(false)
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

    override fun render(
        state: EntryViewState,
        savedState: UiSavedState
    ) {
        state.isSettingsItemVisible.let { show ->
            settingsItem?.isVisible = show
        }

        state.appNameRes.let { name ->
            toolbarActivity.withToolbar { toolbar ->
                if (name == 0) {
                    toolbar.title = null
                } else {
                    toolbar.setTitle(name)
                }
            }
        }
    }
}
