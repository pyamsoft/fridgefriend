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
import com.pyamsoft.fridge.core.applyToolbarOffset
import com.pyamsoft.pydroid.ui.settings.AppSettingsFragment
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import timber.log.Timber

internal class SettingsFragment : AppSettingsFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applyToolbarOffset()
    }

    override fun provideSettingsFragment(): AppSettingsPreferenceFragment {
        return SettingsPreferenceFragment()
    }

    override fun provideSettingsTag(): String {
        return SettingsPreferenceFragment.TAG
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

        override val preferenceXmlResId: Int = R.xml.preferences

        override fun onDestroyView() {
            super.onDestroyView()
            Timber.d("OnDestroyView: $viewLifecycleOwner ${viewLifecycleOwner.lifecycle.currentState}")
        }

        override fun onDestroy() {
            super.onDestroy()
            Timber.d("OnDestroy: ${this.lifecycle} ${this.lifecycle.currentState}")
        }

        companion object {

            const val TAG = "SettingsPreferenceFragment"
        }
    }
}
