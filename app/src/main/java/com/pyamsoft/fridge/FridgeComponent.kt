/*
 * Copyright 2020 Peter Kenji Yamanaka
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
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.injector.ButlerComponent
import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.workmanager.WorkManagerModule
import com.pyamsoft.fridge.category.CategoryComponent
import com.pyamsoft.fridge.category.CategoryListComponent
import com.pyamsoft.fridge.core.R
import com.pyamsoft.fridge.db.DbModule
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.fridge.db.persist.PersistentCategoryPreferences
import com.pyamsoft.fridge.db.room.RoomModule
import com.pyamsoft.fridge.detail.DetailComponent
import com.pyamsoft.fridge.detail.DetailListComponent
import com.pyamsoft.fridge.detail.expand.ExpandComponent
import com.pyamsoft.fridge.detail.expand.ExpandItemCategoryListComponent
import com.pyamsoft.fridge.detail.expand.date.DateSelectComponent
import com.pyamsoft.fridge.detail.expand.date.DateSelectPayload
import com.pyamsoft.fridge.detail.expand.move.ItemMoveComponent
import com.pyamsoft.fridge.entry.EntryComponent
import com.pyamsoft.fridge.entry.EntryListComponent
import com.pyamsoft.fridge.entry.EntryPreferences
import com.pyamsoft.fridge.entry.create.CreateEntryComponent
import com.pyamsoft.fridge.locator.GpsChangeEvent
import com.pyamsoft.fridge.locator.LocatorModule
import com.pyamsoft.fridge.locator.map.MapModule
import com.pyamsoft.fridge.locator.map.osm.popup.StoreInfoComponent
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoComponent
import com.pyamsoft.fridge.main.MainComponent
import com.pyamsoft.fridge.map.MapComponent
import com.pyamsoft.fridge.permission.PermissionComponent
import com.pyamsoft.fridge.preference.PreferencesImpl
import com.pyamsoft.fridge.search.SearchComponent
import com.pyamsoft.fridge.setting.SettingsComponent
import com.pyamsoft.fridge.setting.SettingsPreferences
import com.pyamsoft.fridge.ui.UiModule
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import dagger.Binds
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
        DbModule::class,
        ButlerModule::class,
        LocatorModule::class,
        WorkManagerModule::class,
        RoomModule::class,
        MapModule::class,
        UiModule::class
    ]
)
internal interface FridgeComponent {

    //  @CheckResult
    //  fun plusScannerComponent(): OcrComponent.Factory

    // ===============================================
    // HACKY INJECTORS

    /* FROM inside CategoryListView: See FridgeFriend Injector */
    @CheckResult
    fun plusCategoryListComponent(): CategoryListComponent.Factory

    /* FROM inside DetailList: See FridgeFriend Injector */
    @CheckResult
    fun plusDetailListComponent(): DetailListComponent.Factory

    /* FROM inside EntryList: See FridgeFriend Injector */
    @CheckResult
    fun plusEntryListComponent(): EntryListComponent.Factory

    /* FROM inside ExpandItemCategoryList: See FridgeFriend Injector */
    @CheckResult
    fun plusExpandCategoryListComponent(): ExpandItemCategoryListComponent.Factory

    /* FROM inside OsmMap: See FridgeFriend Injector */
    @CheckResult
    fun plusStoreComponent(): StoreInfoComponent.Factory

    /* FROM inside OsmMap: See FridgeFriend Injector */
    @CheckResult
    fun plusZoneComponent(): ZoneInfoComponent.Factory

    /* FROM inside LocationInjector, ExpirationInjector: See FridgeFriend Injector */
    @CheckResult
    fun plusButlerComponent(): ButlerComponent

    // ===============================================

    @CheckResult
    fun plusSettingsComponent(): SettingsComponent.Factory

    @CheckResult
    fun plusCategoryComponent(): CategoryComponent.Factory

    @CheckResult
    fun plusItemMoveComponent(): ItemMoveComponent.Factory

    @CheckResult
    fun plusExpandComponent(): ExpandComponent.Factory

    @CheckResult
    fun plusDetailComponent(): DetailComponent.Factory

    @CheckResult
    fun plusSearchComponent(): SearchComponent.Factory

    @CheckResult
    fun plusEntryComponent(): EntryComponent.Factory

    @CheckResult
    fun plusCreateEntryComponent(): CreateEntryComponent.Factory

    @CheckResult
    fun plusMainComponent(): MainComponent.Factory

    @CheckResult
    fun plusMapComponent(): MapComponent.Factory

    @CheckResult
    fun plusPermissionComponent(): PermissionComponent.Factory

    @CheckResult
    fun plusDateSelectComponent(): DateSelectComponent

    fun inject(application: FridgeFriend)

    @Component.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance application: Application,
            @Named("debug") @BindsInstance debug: Boolean,
            @BindsInstance theming: Theming,
            @BindsInstance imageLoader: ImageLoader,
            @BindsInstance activityClass: Class<out Activity>
        ): FridgeComponent
    }

    @Module
    abstract class FridgeProvider {

        @Binds
        internal abstract fun bindButlerPreferences(impl: PreferencesImpl): ButlerPreferences

        @Binds
        internal abstract fun bindNotificationPreferences(impl: PreferencesImpl): NotificationPreferences

        @Binds
        internal abstract fun bindDetailPreferences(impl: PreferencesImpl): FridgeItemPreferences

        @Binds
        internal abstract fun bindPersistentCategoryPreferences(impl: PreferencesImpl): PersistentCategoryPreferences

        @Binds
        internal abstract fun bindSettingsPreferences(impl: PreferencesImpl): SettingsPreferences

        @Binds
        internal abstract fun bindEntryPreferences(impl: PreferencesImpl): EntryPreferences

        @Module
        companion object {

            @Provides
            @JvmStatic
            @Singleton
            internal fun provideGpsStateBus(): EventBus<GpsChangeEvent> {
                return EventBus.create(emitOnlyWhenActive = true)
            }

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
