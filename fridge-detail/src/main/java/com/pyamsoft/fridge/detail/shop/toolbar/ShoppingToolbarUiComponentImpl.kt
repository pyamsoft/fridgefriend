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

package com.pyamsoft.fridge.detail.shop.toolbar

import android.os.Bundle
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarUiComponent.Callback
import com.pyamsoft.pydroid.arch.BaseUiComponent
import com.pyamsoft.pydroid.arch.doOnDestroy
import com.pyamsoft.pydroid.ui.arch.InvalidIdException
import javax.inject.Inject

internal class ShoppingToolbarUiComponentImpl @Inject internal constructor(
  private val toolbar: ShoppingToolbar,
  private val binder: ShoppingToolbarBinder
) : BaseUiComponent<ShoppingToolbarUiComponent.Callback>(),
  ShoppingToolbarUiComponent,
  ShoppingToolbarBinder.Callback {

  override fun id(): Int {
    throw InvalidIdException
  }

  override fun onBind(owner: LifecycleOwner, savedInstanceState: Bundle?, callback: Callback) {
    owner.doOnDestroy {
      toolbar.teardown()
      binder.unbind()
    }

    toolbar.inflate(savedInstanceState)
    binder.bind(this)
  }

  override fun onSaveState(outState: Bundle) {
    toolbar.saveState(outState)
  }

  override fun handleBack() {
    callback.onBack()
  }

}
