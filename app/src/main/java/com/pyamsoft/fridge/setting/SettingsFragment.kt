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
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.applyToolbarOffset
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.settings.AppSettingsFragment
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import javax.inject.Inject

internal class SettingsFragment : AppSettingsFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applyToolbarOffset()

        initializeApp()
    }

    private fun initializeApp() {
        val act = requireActivity()
        if (act is VersionChecker) {
            act.checkVersionForUpdate()
        }
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

        @JvmField
        @Inject
        internal var factory: ViewModelProvider.Factory? = null
        private val viewModel by viewModelFactory<SettingsViewModel> { factory }

        @JvmField
        @Inject
        internal var spacer: SettingsSpacer? = null

        private var stateSaver: StateSaver? = null

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            Injector.obtain<FridgeComponent>(view.context.applicationContext)
                .plusSettingsComponent()
                .create(preferenceScreen)
                .inject(this)

            stateSaver = createComponent(
                savedInstanceState,
                viewLifecycleOwner,
                viewModel,
                requireNotNull(spacer)
            ) {}
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            stateSaver?.saveState(outState)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            factory = null
            stateSaver = null
        }

        companion object {

            const val TAG = "SettingsPreferenceFragment"
        }
    }
}
