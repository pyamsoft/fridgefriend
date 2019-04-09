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

import com.pyamsoft.fridge.detail.shop.ShoppingScope
import com.pyamsoft.pydroid.arch.UiBinder
import javax.inject.Inject

@ShoppingScope
internal class ShoppingToolbarBinder @Inject internal constructor(
) : UiBinder<ShoppingToolbarBinder.Callback>(),
  ShoppingToolbar.Callback {

  override fun onBind() {
  }

  override fun onUnbind() {
  }

  override fun onNavigationClicked() {
    callback.handleBack()
  }

  interface Callback : UiBinder.Callback {

    fun handleBack()
  }

}
