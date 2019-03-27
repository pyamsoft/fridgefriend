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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.detail.list.DetailList
import com.pyamsoft.fridge.detail.list.DetailListPresenter
import com.pyamsoft.fridge.detail.list.DetailListUiComponent
import com.pyamsoft.fridge.detail.list.DetailListUiComponentImpl
import com.pyamsoft.fridge.detail.title.DetailTitle
import com.pyamsoft.fridge.detail.title.DetailTitlePresenter
import com.pyamsoft.fridge.detail.title.DetailTitleUiComponent
import com.pyamsoft.fridge.detail.title.DetailTitleUiComponentImpl
import com.pyamsoft.fridge.detail.toolbar.DetailToolbar
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarPresenter
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarUiComponent
import com.pyamsoft.fridge.detail.toolbar.DetailToolbarUiComponentImpl
import dagger.Binds
import dagger.Module

@Module
abstract class DetailModule {

  @Binds
  @CheckResult
  internal abstract fun bindToolbarCallback(impl: DetailToolbarPresenter): DetailToolbar.Callback

  @Binds
  @CheckResult
  internal abstract fun bindToolbarComponent(impl: DetailToolbarUiComponentImpl): DetailToolbarUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindDetailCallback(impl: DetailTitlePresenter): DetailTitle.Callback

  @Binds
  @CheckResult
  internal abstract fun bindDetailComponent(impl: DetailTitleUiComponentImpl): DetailTitleUiComponent

  @Binds
  @CheckResult
  internal abstract fun bindListCallback(impl: DetailListPresenter): DetailList.Callback

  @Binds
  @CheckResult
  internal abstract fun bindListComponent(impl: DetailListUiComponentImpl): DetailListUiComponent

}
