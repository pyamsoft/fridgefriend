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

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.updateLayoutParams
import com.pyamsoft.fridge.core.Core
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UnitViewEvent
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.privacy.addPrivacy
import com.pyamsoft.pydroid.ui.privacy.removePrivacy
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject
import javax.inject.Named

class MainToolbar @Inject internal constructor(
  private val toolbarActivityProvider: ToolbarActivityProvider,
  private val theming: Theming,
  @Named("app_name") private val appNameRes: Int,
  activity: Activity,
  parent: ViewGroup
) : BaseUiView<UnitViewState, UnitViewEvent>(parent) {

  override val layout: Int = R.layout.main_toolbar

  override val layoutRoot by boundView<Toolbar>(R.id.main_toolbar)

  private var activity: Activity? = activity

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    inflateToolbar()

    layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
      v.updateLayoutParams<MarginLayoutParams> {
        topMargin = padding.top + insets.systemWindowInsetTop + 8.toDp(v.context)
      }
    }

    layoutRoot.addPrivacy(Core.PRIVACY_POLICY_URL, Core.TERMS_CONDITIONS_URL)
  }

  override fun onRender(
    state: UnitViewState,
    savedState: UiSavedState
  ) {
  }

  override fun onTeardown() {
    layoutRoot.removePrivacy()
    toolbarActivityProvider.setToolbar(null)
    activity = null
  }

  private fun inflateToolbar() {
    val theme: Int
    if (theming.isDarkTheme(requireNotNull(activity))) {
      theme = R.style.ThemeOverlay_MaterialComponents
    } else {
      theme = R.style.ThemeOverlay_MaterialComponents_Light
    }

    layoutRoot.apply {
      popupTheme = theme
      setTitle(appNameRes)
      ViewCompat.setElevation(this, 4f.toDp(context).toFloat())
      toolbarActivityProvider.setToolbar(this)
    }
  }
}
