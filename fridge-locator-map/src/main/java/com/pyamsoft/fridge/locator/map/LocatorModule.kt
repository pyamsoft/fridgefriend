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

package com.pyamsoft.fridge.locator.map

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.map.gms.GmsLocator
import com.pyamsoft.fridge.locator.map.osm.api.NearbyLocationApi
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay
import com.pyamsoft.fridge.locator.map.permission.PermissionGranter
import com.pyamsoft.fridge.locator.map.permission.PermissionHandlerImpl
import com.pyamsoft.fridge.locator.permission.BackgroundLocationPermission
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class LocatorModule {

    @Binds
    @CheckResult
    internal abstract fun bindLocator(impl: GmsLocator): Locator

    @Binds
    @CheckResult
    internal abstract fun bindLocationPermissions(impl: PermissionGranter): MapPermission

    @Binds
    @CheckResult
    internal abstract fun bindDeviceGps(impl: GmsLocator): DeviceGps

    @Binds
    @CheckResult
    internal abstract fun bindGeofencer(impl: GmsLocator): Geofencer

    @Module
    companion object {

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideForegroundHandler(): PermissionHandler<ForegroundLocationPermission> {
            return PermissionHandlerImpl(ForegroundLocationPermission)
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideBackgroundHandler(): PermissionHandler<BackgroundLocationPermission> {
            return PermissionHandlerImpl(BackgroundLocationPermission)
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideRetrofit(
            @Named("debug") debug: Boolean,
            moshi: Moshi
        ): Retrofit {
            val baseUrl = "https://overpass-api.de/api/"
            val client = OkHttpClient.Builder()
                .apply {
                    if (debug) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = BODY
                        })
                    }
                }
                .build()
            val newMoshi = moshi.newBuilder()
                .add(OsmNodeOrWay.Adapter())
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(MoshiConverterFactory.create(newMoshi))
                .client(client)
                .build()
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideNearbyLocationApi(retrofit: Retrofit): NearbyLocationApi {
            return retrofit.create(NearbyLocationApi::class.java)
        }
    }
}
