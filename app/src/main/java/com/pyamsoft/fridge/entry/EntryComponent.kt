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

import android.view.ViewGroup
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.entry.EntryComponent.EntryProvider
import com.pyamsoft.fridge.entry.impl.EntryModule
import com.pyamsoft.pydroid.ui.app.ToolbarActivityProvider
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import javax.inject.Named

@EntryScope
@Subcomponent(modules = [EntryProvider::class, EntryModule::class])
internal interface EntryComponent {

  fun inject(activity: EntryActivity)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun parent(parent: ViewGroup): Builder

    @BindsInstance
    @CheckResult
    fun toolbarActivityProvider(provider: ToolbarActivityProvider): Builder

    @CheckResult
    fun build(): EntryComponent
  }

  @Module
  object EntryProvider {

    @Provides
    @JvmStatic
    fun provideDropshadow(parent: ViewGroup): DropshadowView {
      return DropshadowView(parent)
    }

    @Provides
    @JvmStatic
    @Named("app_name")
    fun provideAppNameRes(): Int {
      return R.string.app_name
    }

  }

}
