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

package com.pyamsoft.fridge.locator.map.osm.popup.zone

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import com.pyamsoft.fridge.locator.map.osm.popup.PopupInfoScope
import com.pyamsoft.fridge.locator.map.osm.popup.PopupViewModelFactory
import com.pyamsoft.fridge.locator.map.osm.popup.ViewModelKey
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoComponent.ViewModelModule
import com.pyamsoft.pydroid.arch.UiViewModel
import dagger.Binds
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.multibindings.IntoMap

@PopupInfoScope
@Component(modules = [ViewModelModule::class])
internal interface ZoneInfoComponent {

  fun inject(infoWindow: ZoneInfoWindow)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance parent: ViewGroup,
      @BindsInstance zone: NearbyZone,
      @BindsInstance nearbyZoneRealtime: NearbyZoneRealtime,
      @BindsInstance nearbyZoneQueryDao: NearbyZoneQueryDao,
      @BindsInstance nearbyZoneInsertDao: NearbyZoneInsertDao,
      @BindsInstance nearbyZoneDeleteDao: NearbyZoneDeleteDao
    ): ZoneInfoComponent
  }

  @Module
  abstract class ViewModelModule {

    @Binds
    internal abstract fun bindViewModelFactory(factory: PopupViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @ViewModelKey(ZoneInfoViewModel::class)
    internal abstract fun zoneViewModel(viewModel: ZoneInfoViewModel): UiViewModel<*, *, *>
  }

}
