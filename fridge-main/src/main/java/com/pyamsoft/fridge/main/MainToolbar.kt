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

package com.pyamsoft.fridge.main

import android.view.MenuItem
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.pyamsoft.fridge.core.Core
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.privacy.addPrivacy
import com.pyamsoft.pydroid.ui.privacy.removePrivacy
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject
import javax.inject.Named

class MainToolbar @Inject internal constructor(
    @Named("app_name") appNameRes: Int,
    toolbarActivityProvider: ToolbarActivityProvider,
    theming: ThemeProvider,
    parent: ViewGroup
) : BaseUiView<MainViewState, MainViewEvent>(parent) {

    override val layout: Int = R.layout.main_toolbar

    override val layoutRoot by boundView<Toolbar>(R.id.main_toolbar)

    private var settingsItem: MenuItem? = null

    init {
        doOnInflate {
            inflateToolbar(toolbarActivityProvider, theming, appNameRes)

            layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
                v.updateLayoutParams<MarginLayoutParams> {
                    topMargin = padding.top + insets.systemWindowInsetTop + 8.toDp(v.context)
                }
            }

            layoutRoot.addPrivacy(Core.PRIVACY_POLICY_URL, Core.TERMS_CONDITIONS_URL)
        }

        doOnTeardown {
            layoutRoot.removePrivacy()
            toolbarActivityProvider.setToolbar(null)
        }

        doOnInflate {
            inflateMenu()
        }

        doOnTeardown {
            teardownMenu()
        }
    }

    override fun onRender(
        state: MainViewState,
        savedState: UiSavedState
    ) {
        state.isSettingsItemVisible.let { show ->
            settingsItem?.isVisible = show
        }

        state.appNameRes.let { name ->
            if (name == 0) {
                layoutRoot.title = null
            } else {
                layoutRoot.setTitle(name)
            }
        }
    }

    private fun teardownMenu() {
        settingsItem?.setOnMenuItemClickListener(null)
        settingsItem = null
        layoutRoot.menu.removeItem(R.id.menu_item_settings)
    }

    private fun inflateMenu() {
        layoutRoot.let { toolbar ->
            toolbar.setUpEnabled(false)
            toolbar.inflateMenu(R.menu.toolbar_menu)
            toolbar.menu.findItem(R.id.menu_item_settings)
                .also { item ->
                    item.setOnMenuItemClickListener {
                        publish(MainViewEvent.SettingsNavigate)
                        return@setOnMenuItemClickListener true
                    }
                    settingsItem = item
                }
        }
    }

    private fun inflateToolbar(
        toolbarActivityProvider: ToolbarActivityProvider,
        theming: ThemeProvider,
        appNameRes: Int
    ) {
        val theme = if (theming.isDarkTheme()) {
            R.style.ThemeOverlay_MaterialComponents
        } else {
            R.style.ThemeOverlay_MaterialComponents_Light
        }

        layoutRoot.apply {
            popupTheme = theme
            setTitle(appNameRes)
            ViewCompat.setElevation(this, 8f.toDp(context).toFloat())
            toolbarActivityProvider.setToolbar(this)
        }
    }
}
