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

package com.pyamsoft.fridge

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.FridgeComponent.FridgeProvider
import com.pyamsoft.fridge.detail.DetailComponent
import com.pyamsoft.fridge.entry.EntryComponent
import com.pyamsoft.fridge.main.MainComponent
import com.pyamsoft.fridge.setting.SettingComponent
import com.pyamsoft.pydroid.core.threads.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(modules = [FridgeProvider::class])
internal interface FridgeComponent {

  @CheckResult
  fun plusDetailComponent(): DetailComponent.Builder

  @CheckResult
  fun plusEntryComponent(): EntryComponent.Builder

  @CheckResult
  fun plusMainComponent(): MainComponent.Builder

  @CheckResult
  fun plusSettingComponent(): SettingComponent.Builder

  @Component.Builder
  interface Builder {

    @BindsInstance
    @CheckResult
    fun theming(theming: Theming): Builder

    @BindsInstance
    @CheckResult
    fun moshi(moshi: Moshi): Builder

    @BindsInstance
    @CheckResult
    fun enforcer(enforcer: Enforcer): Builder

    @BindsInstance
    @CheckResult
    fun application(application: Application): Builder

    @BindsInstance
    @CheckResult
    fun imageLoader(imageLoader: ImageLoader): Builder

    @CheckResult
    fun build(): FridgeComponent

  }

  @Module
  object FridgeProvider {

    @Provides
    @JvmStatic
    @Named("app_name")
    internal fun provideAppNameRes(): Int {
      return R.string.app_name
    }

  }
}

