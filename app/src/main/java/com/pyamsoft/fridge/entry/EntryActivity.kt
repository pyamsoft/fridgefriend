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
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.Injector
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.entry.impl.EntryListUiComponent
import com.pyamsoft.fridge.entry.impl.EntryToolbarUiComponent
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import javax.inject.Inject

internal class EntryActivity : RatingActivity(),
  EntryListUiComponent.Callback,
  EntryToolbarUiComponent.Callback {

  override val applicationIcon: Int = R.mipmap.ic_launcher

  override val versionName: String = BuildConfig.VERSION_NAME

  override val changeLogLines: ChangeLogBuilder = buildChangeLog {

  }

  override val fragmentContainerId: Int
    get() = entryList.id()

  override val snackbarRoot: View
    get() = requireNotNull(snackbarContainer)

  // Nullable to prevent memory leak
  private var snackbarContainer: CoordinatorLayout? = null

  @field:Inject internal lateinit var toolbar: EntryToolbarUiComponent
  @field:Inject internal lateinit var entryList: EntryListUiComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    setDynamicTheme()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.snackbar_screen)

    snackbarContainer = findViewById(R.id.snackbar_container)
    val contentContainer = findViewById<ConstraintLayout>(R.id.content_container)

    Injector.obtain<FridgeComponent>(applicationContext)
      .plusEntryComponent()
      .toolbarActivityProvider(this)
      .parent(contentContainer)
      .build()
      .inject(this)

    inflateComponents(savedInstanceState)
    layoutComponents(contentContainer)
  }

  private fun setDynamicTheme() {
    @StyleRes val theme: Int
    if (ThemeInjector.obtain(applicationContext).isDarkTheme()) {
      theme = R.style.Theme_Fridge_Dark_Normal
    } else {
      theme = R.style.Theme_Fridge_Light_Normal
    }

    setTheme(theme)
  }

  private fun inflateComponents(savedInstanceState: Bundle?) {
    entryList.bind(this, savedInstanceState, this)
    toolbar.bind(this, savedInstanceState, this)
  }

  private fun layoutComponents(contentContainer: ConstraintLayout) {
    toolbar.layout(contentContainer)
    entryList.layout(contentContainer, toolbar.id())
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    entryList.saveState(outState)
    toolbar.saveState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    snackbarContainer = null
  }

}