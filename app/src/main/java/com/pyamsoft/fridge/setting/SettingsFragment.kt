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

import com.pyamsoft.pydroid.ui.settings.AppSettingsFragment
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment

internal class SettingsFragment : AppSettingsFragment() {

  override fun provideSettingsFragment(): AppSettingsPreferenceFragment {
    return SettingsPreferenceFragment()
  }

  override fun provideSettingsTag(): String {
    return SettingsPreferenceFragment.TAG
  }

  companion object {

    const val TAG = "SettingsFragment"
  }

  internal class SettingsPreferenceFragment : AppSettingsPreferenceFragment() {

    companion object {

      const val TAG = "SettingsPreferenceFragment"
    }

  }

}
