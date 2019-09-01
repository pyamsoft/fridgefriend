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
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.pyamsoft.fridge.setting.SettingViewEvent.Navigate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class SettingToolbar @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<UnitViewState, SettingViewEvent>(parent) {

    override val layout: Int = R.layout.setting_toolbar

    override val layoutRoot by boundView<Toolbar>(R.id.setting_toolbar)

    override fun onInflated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        layoutRoot.title = "Settings"
        layoutRoot.setUpEnabled(true)
        layoutRoot.setNavigationOnClickListener(DebouncedOnClickListener.create {
            publish(Navigate)
        })
    }

    override fun onRender(
        state: UnitViewState,
        savedState: UiSavedState
    ) {
    }

    override fun onTeardown() {
        layoutRoot.setUpEnabled(false)
        layoutRoot.setNavigationOnClickListener(null)
    }
}
