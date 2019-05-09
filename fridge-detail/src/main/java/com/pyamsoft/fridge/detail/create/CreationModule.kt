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

package com.pyamsoft.fridge.detail.create

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.detail.create.list.CreationList
import com.pyamsoft.fridge.detail.create.list.CreationListHandler
import com.pyamsoft.fridge.detail.create.list.CreationListHandler.CreationEvent
import com.pyamsoft.fridge.detail.create.list.CreationListUiComponentImpl
import com.pyamsoft.fridge.detail.create.title.CreationTitle
import com.pyamsoft.fridge.detail.create.title.CreationTitleHandler
import com.pyamsoft.fridge.detail.create.title.CreationTitleHandler.TitleEvent
import com.pyamsoft.fridge.detail.create.title.CreationTitleUiComponent
import com.pyamsoft.fridge.detail.create.title.CreationTitleUiComponentImpl
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbar
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarHandler
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarHandler.ToolbarEvent
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarUiComponent
import com.pyamsoft.fridge.detail.create.toolbar.CreationToolbarUiComponentImpl
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.fridge.detail.list.DetailListUiComponent
import com.pyamsoft.pydroid.arch.UiEventHandler
import dagger.Binds
import dagger.Module

@Module
abstract class CreationModule {

  @Binds
  @CheckResult
  internal abstract fun bindListComponent(impl: CreationListUiComponentImpl): DetailListUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindListCallback(impl: CreationListHandler): DetailList.Callback

  @Binds
  @CheckResult
  internal abstract fun bindListHandler(impl: CreationListHandler): UiEventHandler<CreationEvent, DetailList.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindToolbarCallback(impl: CreationToolbarHandler): CreationToolbar.Callback

  @Binds
  @CheckResult
  internal abstract fun bindToolbarHandler(impl: CreationToolbarHandler): UiEventHandler<ToolbarEvent, CreationToolbar.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindToolbarComponent(impl: CreationToolbarUiComponentImpl): CreationToolbarUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindTitleCallback(impl: CreationTitleHandler): CreationTitle.Callback

  @Binds
  @CheckResult
  internal abstract fun bindTitleHandler(impl: CreationTitleHandler): UiEventHandler<TitleEvent, CreationTitle.Callback>

  @Binds
  @CheckResult
  internal abstract fun bindTitleComponent(impl: CreationTitleUiComponentImpl): CreationTitleUiComponent

}
