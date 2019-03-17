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

package com.pyamsoft.fridge.create

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.create.title.CreateTitle
import com.pyamsoft.fridge.create.title.CreateTitlePresenter
import com.pyamsoft.fridge.create.title.CreateTitleUiComponent
import com.pyamsoft.fridge.create.title.CreateTitleUiComponentImpl
import com.pyamsoft.fridge.create.toolbar.CreateToolbar
import com.pyamsoft.fridge.create.toolbar.CreateToolbarPresenter
import com.pyamsoft.fridge.create.toolbar.CreateToolbarUiComponent
import com.pyamsoft.fridge.create.toolbar.CreateToolbarUiComponentImpl
import dagger.Binds
import dagger.Module

@Module
abstract class CreateModule {

  @Binds
  @CheckResult
  internal abstract fun bindToolbarCallback(impl: CreateToolbarPresenter): CreateToolbar.Callback

  @Binds
  @CheckResult
  internal abstract fun bindToolbarComponent(impl: CreateToolbarUiComponentImpl): CreateToolbarUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindCreateCallback(impl: CreateTitlePresenter): CreateTitle.Callback

  @Binds
  @CheckResult
  internal abstract fun bindCreateComponent(impl: CreateTitleUiComponentImpl): CreateTitleUiComponent

}
