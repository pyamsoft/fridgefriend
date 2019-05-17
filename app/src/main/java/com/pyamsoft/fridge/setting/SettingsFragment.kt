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

package com.pyamsoft.fridge.setting

import android.os.Bundle
import android.view.View
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.setting.SettingToolbarControllerEvent.NavigateUp
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireToolbarActivity
import com.pyamsoft.pydroid.ui.settings.AppSettingsFragment
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import javax.inject.Inject

internal class SettingsFragment : AppSettingsFragment() {

  @JvmField @Inject internal var toolbar: SettingToolbar? = null
  @JvmField @Inject internal var viewModel: SettingToolbarViewModel? = null

  override fun provideSettingsFragment(): AppSettingsPreferenceFragment {
    return SettingsPreferenceFragment()
  }

  override fun provideSettingsTag(): String {
    return SettingsPreferenceFragment.TAG
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusSettingComponent()
        .create(requireToolbarActivity())
        .inject(this)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        requireNotNull(viewModel),
        requireNotNull(toolbar)
    ) {
      return@createComponent when (it) {
        is NavigateUp -> requireActivity().onBackPressed()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    toolbar?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    toolbar = null
  }

  companion object {

    const val TAG = "SettingsFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return SettingsFragment().apply {
        arguments = Bundle().apply {
        }
      }
    }
  }

  internal class SettingsPreferenceFragment : AppSettingsPreferenceFragment() {

    companion object {

      const val TAG = "SettingsPreferenceFragment"
    }

  }

}
