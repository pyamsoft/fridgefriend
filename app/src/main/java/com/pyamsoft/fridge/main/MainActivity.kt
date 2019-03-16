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

import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.Injector
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.entry.EntryListFragment
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.theme.ThemeInjector
import com.pyamsoft.pydroid.ui.util.commit
import javax.inject.Inject

internal class MainActivity : RatingActivity(),
  MainToolbarUiComponent.Callback {

  override val applicationIcon: Int = R.mipmap.ic_launcher

  override val versionName: String = BuildConfig.VERSION_NAME

  override val changeLogLines: ChangeLogBuilder = buildChangeLog {

  }

  override val fragmentContainerId: Int
    get() = container.id()

  override val snackbarRoot: View
    get() = requireNotNull(snackbarContainer)

  // Nullable to prevent memory leak
  private var snackbarContainer: CoordinatorLayout? = null

  @field:Inject internal lateinit var toolbar: MainToolbarUiComponent
  @field:Inject internal lateinit var container: FragmentContainerUiComponent

  override fun onCreate(savedInstanceState: Bundle?) {
    setDynamicTheme()
    super.onCreate(savedInstanceState)
    setContentView(R.layout.snackbar_screen)

    snackbarContainer = findViewById(R.id.snackbar_container)
    val contentContainer = findViewById<ConstraintLayout>(R.id.content_container)

    Injector.obtain<FridgeComponent>(applicationContext)
      .plusMainComponent()
      .toolbarActivityProvider(this)
      .parent(contentContainer)
      .build()
      .inject(this)

    inflateComponents(contentContainer, savedInstanceState)

    pushFragment()
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

  private fun inflateComponents(constraintLayout: ConstraintLayout, savedInstanceState: Bundle?) {
    container.bind(this, savedInstanceState, Unit)
    toolbar.bind(this, savedInstanceState, this)

    toolbar.layout(constraintLayout)
    container.layout(constraintLayout, toolbar.id())
  }

  private fun pushFragment() {
    val fm = supportFragmentManager
    if (fm.findFragmentById(fragmentContainerId) == null) {
      fm.beginTransaction().add(
        fragmentContainerId,
        EntryListFragment.newInstance(),
        EntryListFragment.TAG
      ).commit(this)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    toolbar.saveState(outState)
    container.saveState(outState)
  }

  override fun onDestroy() {
    super.onDestroy()
    snackbarContainer = null
  }

}
