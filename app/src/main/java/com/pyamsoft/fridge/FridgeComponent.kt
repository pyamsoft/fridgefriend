/*
 * Copyright 2021 Peter Kenji Yamanaka
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
 */

package com.pyamsoft.fridge

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.FridgeComponent.FridgeProvider
import com.pyamsoft.fridge.butler.ButlerModule
import com.pyamsoft.fridge.butler.injector.ButlerComponent
import com.pyamsoft.fridge.butler.workmanager.WorkManagerModule
import com.pyamsoft.fridge.category.CategoryComponent
import com.pyamsoft.fridge.category.item.CategoryItemComponent
import com.pyamsoft.fridge.core.R
import com.pyamsoft.fridge.db.DbModule
import com.pyamsoft.fridge.db.room.RoomModule
import com.pyamsoft.fridge.detail.DetailComponent
import com.pyamsoft.fridge.detail.expand.ExpandComponent
import com.pyamsoft.fridge.detail.expand.categories.ExpandCategoryComponent
import com.pyamsoft.fridge.detail.expand.date.DateSelectComponent
import com.pyamsoft.fridge.detail.expand.date.DateSelectPayload
import com.pyamsoft.fridge.detail.expand.move.ItemMoveComponent
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.entry.EntryComponent
import com.pyamsoft.fridge.entry.create.CreateEntryComponent
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.main.MainComponent
import com.pyamsoft.fridge.preference.PreferenceModule
import com.pyamsoft.fridge.search.SearchComponent
import com.pyamsoft.fridge.setting.SettingsComponent
import com.pyamsoft.fridge.ui.UiModule
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules =
        [
            FridgeProvider::class,
            PreferenceModule::class,
            DbModule::class,
            ButlerModule::class,
            WorkManagerModule::class,
            RoomModule::class,
            UiModule::class])
internal interface FridgeComponent {

  /** Not actually used, just here so graph can compile */
  @CheckResult
  @Suppress("FunctionName")
  fun `$$daggerRequiredEntryItemComponent`(): EntryItemComponent.Factory

  /** Not actually used, just here so graph can compile */
  @CheckResult
  @Suppress("FunctionName")
  fun `$$daggerRequiredDetailItemComponent`(): DetailItemComponent.Factory

  /** Not actually used, just here so graph can compile */
  @CheckResult
  @Suppress("FunctionName")
  fun `$$daggerRequiredExpandCategoryComponent`(): ExpandCategoryComponent.Factory

  /** Not actually used, just here so graph can compile */
  @CheckResult
  @Suppress("FunctionName")
  fun `$$daggerRequiredCategoryItemComponent`(): CategoryItemComponent.Factory

  // ===============================================
  // HACKY INJECTORS

  /* FROM inside NightlyInjector, ItemInjector: See FridgeFriend Injector */
  @CheckResult fun plusButlerComponent(): ButlerComponent

  // ===============================================

  @CheckResult fun plusSettingsComponent(): SettingsComponent.Factory

  @CheckResult fun plusCategoryComponent(): CategoryComponent.Factory

  @CheckResult fun plusItemMoveComponent(): ItemMoveComponent.Factory

  @CheckResult fun plusExpandComponent(): ExpandComponent.Factory

  @CheckResult fun plusDetailComponent(): DetailComponent.Factory

  @CheckResult fun plusSearchComponent(): SearchComponent.Factory

  @CheckResult fun plusEntryComponent(): EntryComponent.Factory

  @CheckResult fun plusCreateEntryComponent(): CreateEntryComponent.Factory

  @CheckResult fun plusMainComponent(): MainComponent.Factory

  @CheckResult fun plusDateSelectComponent(): DateSelectComponent

  fun inject(application: FridgeFriend)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance application: Application,
        @Named("debug") @BindsInstance debug: Boolean,
        @BindsInstance theming: Theming,
        @BindsInstance imageLoader: ImageLoader,
        @BindsInstance activityClass: Class<out Activity>,
    ): FridgeComponent
  }

  @Module
  abstract class FridgeProvider {

    @Module
    companion object {

      @Provides
      @JvmStatic
      @Singleton
      internal fun provideDateSelectBus(): EventBus<DateSelectPayload> {
        return EventBus.create(emitOnlyWhenActive = true)
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
}
