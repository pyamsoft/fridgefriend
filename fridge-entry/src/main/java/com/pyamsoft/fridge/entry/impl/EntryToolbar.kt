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

package com.pyamsoft.fridge.entry.impl

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import com.pyamsoft.fridge.entry.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject
import javax.inject.Named

internal class EntryToolbar @Inject internal constructor(
  private val toolbarActivityProvider: ToolbarActivityProvider,
  private val theming: Theming,
  @Named("app_name") private val appNameRes: Int,
  parent: ViewGroup,
  callback: EntryToolbar.Callback
) : BaseUiView<EntryToolbar.Callback>(parent, callback) {

  override val layout: Int = R.layout.colored_toolbar

  override val layoutRoot by lazyView<Toolbar>(R.id.colored_toolbar)

  override fun onInflated(view: View, savedInstanceState: Bundle?) {
    inflateToolbar()
  }

  private fun inflateToolbar() {
    val theme: Int
    if (theming.isDarkTheme()) {
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

  override fun onTeardown() {
    toolbarActivityProvider.setToolbar(null)
  }

  interface Callback {

  }
}