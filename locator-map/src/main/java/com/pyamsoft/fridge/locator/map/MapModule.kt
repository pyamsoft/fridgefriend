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

package com.pyamsoft.fridge.locator.map

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.map.gms.GmsLocator
import com.pyamsoft.fridge.locator.map.osm.popup.StoreInfoInteractorImpl
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoInteractorImpl
import com.pyamsoft.fridge.locator.map.popup.store.StoreInfoInteractor
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoInteractor
import dagger.Binds
import dagger.Module

@Module
abstract class MapModule {

    @Binds
    @CheckResult
    internal abstract fun bindDeviceGps(impl: GmsLocator): DeviceGps

    @Binds
    @CheckResult
    internal abstract fun bindGeofencer(impl: GmsLocator): Geofencer

    @Binds
    @CheckResult
    internal abstract fun bindZoneInteractor(impl: ZoneInfoInteractorImpl): ZoneInfoInteractor

    @Binds
    @CheckResult
    internal abstract fun bindStoreInteractor(impl: StoreInfoInteractorImpl): StoreInfoInteractor
}
