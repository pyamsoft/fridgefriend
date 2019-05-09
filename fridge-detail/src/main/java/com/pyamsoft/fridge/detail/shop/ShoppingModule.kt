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

package com.pyamsoft.fridge.detail.shop

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.fridge.detail.list.DetailListUiComponent
import com.pyamsoft.fridge.detail.shop.list.ShoppingListHandler
import com.pyamsoft.fridge.detail.shop.list.ShoppingListHandler.ShoppingEvent
import com.pyamsoft.fridge.detail.shop.list.ShoppingListUiComponentImpl
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbar
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarHandler
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarUiComponent
import com.pyamsoft.fridge.detail.shop.toolbar.ShoppingToolbarUiComponentImpl
import com.pyamsoft.pydroid.arch.UiEventHandler
import dagger.Binds
import dagger.Module

@Module
abstract class ShoppingModule {

  @Binds
  @CheckResult
  internal abstract fun bindListComponent(impl: ShoppingListUiComponentImpl): DetailListUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindListCallback(impl: ShoppingListHandler): DetailList.Callback

  @Binds
  @CheckResult
  internal abstract fun bindListHandler(impl: ShoppingListHandler): UiEventHandler<ShoppingEvent, DetailList.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindToolbarCallback(impl: ShoppingToolbarHandler): ShoppingToolbar.Callback

  @Binds
  @CheckResult
  internal abstract fun bindToolbarHandler(impl: ShoppingToolbarHandler): UiEventHandler<ToolbarEvent, ShoppingToolbar.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindToolbarComponent(impl: ShoppingToolbarUiComponentImpl): ShoppingToolbarUiComponent

}
