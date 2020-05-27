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
 *
 */

package com.pyamsoft.fridge.locator

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.osm.updatemanager.LocationUpdateManagerImpl
import com.pyamsoft.fridge.locator.osm.updatemanager.LocationUpdatePublisher
import com.pyamsoft.fridge.locator.osm.updatemanager.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionGranter
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.pyamsoft.fridge.locator.permission.PermissionHandlerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
abstract class LocatorModule {

    @Binds
    @CheckResult
    internal abstract fun bindLocationPermissions(impl: PermissionGranter): MapPermission

    @Binds
    @CheckResult
    internal abstract fun bindLocationUpdateReceiver(impl: LocationUpdateManagerImpl): LocationUpdateReceiver

    @Binds
    @CheckResult
    internal abstract fun bindLocationUpdatePublisher(impl: LocationUpdateManagerImpl): LocationUpdatePublisher

    @Module
    companion object {

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideForegroundHandler(): PermissionHandler<ForegroundLocationPermission> {
            return PermissionHandlerImpl(ForegroundLocationPermission)
        }
    }
}
