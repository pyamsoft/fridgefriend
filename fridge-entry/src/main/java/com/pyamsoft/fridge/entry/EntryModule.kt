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

package com.pyamsoft.fridge.entry

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.entry.action.EntryActionCreateHandler
import com.pyamsoft.fridge.entry.action.EntryActionCreateHandler.CreateEvent
import com.pyamsoft.fridge.entry.action.EntryActionShopHandler
import com.pyamsoft.fridge.entry.action.EntryActionShopHandler.ShopEvent
import com.pyamsoft.fridge.entry.action.EntryActionUiComponent
import com.pyamsoft.fridge.entry.action.EntryActionUiComponentImpl
import com.pyamsoft.fridge.entry.action.EntryCreate
import com.pyamsoft.fridge.entry.action.EntryShop
import com.pyamsoft.fridge.entry.list.EntryList
import com.pyamsoft.fridge.entry.list.EntryListHandler
import com.pyamsoft.fridge.entry.list.EntryListHandler.ListEvent
import com.pyamsoft.fridge.entry.list.EntryListUiComponent
import com.pyamsoft.fridge.entry.list.EntryListUiComponentImpl
import com.pyamsoft.fridge.entry.toolbar.EntryToolbar
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarHandler
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarUiComponent
import com.pyamsoft.fridge.entry.toolbar.EntryToolbarUiComponentImpl
import com.pyamsoft.pydroid.arch.UiEventHandler
import dagger.Binds
import dagger.Module

@Module
abstract class EntryModule {

  @Binds
  @CheckResult
  internal abstract fun bindCreateCallback(impl: EntryActionCreateHandler): EntryCreate.Callback

  @Binds
  @CheckResult
  internal abstract fun bindCreateHandler(impl: EntryActionCreateHandler): UiEventHandler<CreateEvent, EntryCreate.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindShopCallback(impl: EntryActionShopHandler): EntryShop.Callback

  @Binds
  @CheckResult
  internal abstract fun bindShopHandler(impl: EntryActionShopHandler): UiEventHandler<ShopEvent, EntryShop.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindActionComponent(impl: EntryActionUiComponentImpl): EntryActionUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindListCallback(impl: EntryListHandler): EntryList.Callback

  @Binds
  @CheckResult
  internal abstract fun bindListHandler(impl: EntryListHandler): UiEventHandler<ListEvent, EntryList.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindListComponent(impl: EntryListUiComponentImpl): EntryListUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindToolbarCallback(impl: EntryToolbarHandler): EntryToolbar.Callback

  @Binds
  @CheckResult
  internal abstract fun bindToolbarHandler(impl: EntryToolbarHandler): UiEventHandler<ToolbarEvent, EntryToolbar.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindToolbarComponent(impl: EntryToolbarUiComponentImpl): EntryToolbarUiComponent

}
