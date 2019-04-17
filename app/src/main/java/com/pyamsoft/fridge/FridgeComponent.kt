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
import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.FridgeComponent.FridgeProvider
import com.pyamsoft.fridge.db.DbProvider
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.room.RoomProvider
import com.pyamsoft.fridge.detail.DetailComponent
import com.pyamsoft.fridge.entry.EntryComponent
import com.pyamsoft.fridge.entry.EntrySingletonModule
import com.pyamsoft.fridge.main.MainComponent
import com.pyamsoft.fridge.ocr.OcrComponent
import com.pyamsoft.fridge.ocr.OcrSingletonModule
import com.pyamsoft.fridge.setting.SettingComponent
import com.pyamsoft.fridge.setting.SettingSingletonModule
import com.pyamsoft.pydroid.core.bus.EventBus
import com.pyamsoft.pydroid.core.bus.RxBus
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
@Component(
  modules = [
    FridgeProvider::class,
    DbProvider::class,
    RoomProvider::class,
    SettingSingletonModule::class,
    OcrSingletonModule::class,
    EntrySingletonModule::class
  ]
)
internal interface FridgeComponent {

  @CheckResult
  fun plusScannerComponent(): OcrComponent.Factory

  @CheckResult
  fun plusDetailComponent(): DetailComponent.Factory

  @CheckResult
  fun plusEntryComponent(): EntryComponent.Factory

  @CheckResult
  fun plusMainComponent(): MainComponent.Factory

  @CheckResult
  fun plusSettingComponent(): SettingComponent.Factory

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance theming: Theming,
      @BindsInstance moshi: Moshi,
      @BindsInstance enforcer: Enforcer,
      @BindsInstance application: Application,
      @BindsInstance imageLoader: ImageLoader
    ): FridgeComponent

  }

  @Module
  object FridgeProvider {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideFakeItemRealtime(): EventBus<FridgeItemChangeEvent> {
      return RxBus.create()
    }

    @Provides
    @JvmStatic
    internal fun provideContext(application: Application): Context {
      return application
    }

    @Provides
    @JvmStatic
    @Named("app_name")
    internal fun provideAppNameRes(): Int {
      return R.string.app_name
    }
  }
}

