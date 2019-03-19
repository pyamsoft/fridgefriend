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

package com.pyamsoft.fridge.setting.toolbar

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import javax.inject.Inject

internal class SettingToolbarUiComponentImpl @Inject internal constructor(
  private val toolbar: SettingToolbar,
  private val presenter: SettingToolbarPresenter
) : BaseUiComponent<SettingToolbarUiComponent.Callback>(),
  SettingToolbarUiComponent,
  SettingToolbarPresenter.Callback {

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun onBind(
    owner: LifecycleOwner,
    savedInstanceState: Bundle?,
    callback: SettingToolbarUiComponent.Callback
  ) {
    owner.doOnDestroy {
      toolbar.teardown()
      presenter.unbind()
    }

    toolbar.inflate(savedInstanceState)
    presenter.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    toolbar.saveState(outState)
  }

  override fun handleNavigateBack() {
    callback.onNavigateBack()
  }

}
