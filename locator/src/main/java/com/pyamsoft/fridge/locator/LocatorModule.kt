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

package com.pyamsoft.fridge.locator

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.api.NearbyLocationApi
import com.pyamsoft.fridge.locator.api.OsmNodeOrWay
import com.pyamsoft.fridge.locator.location.LocationUpdateManagerImpl
import com.pyamsoft.fridge.locator.location.LocationUpdatePublisher
import com.pyamsoft.fridge.locator.location.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.location.NearbyImpl
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionGranter
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.pyamsoft.fridge.locator.permission.PermissionHandlerImpl
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
private annotation class InternalApi

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

    @Binds
    @CheckResult
    internal abstract fun bindNearby(impl: NearbyImpl): Nearby

    @Module
    companion object {

        private const val baseUrl = "https://overpass-api.de/api/"

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideMoshi(): Moshi {
            return Moshi.Builder().add(OsmNodeOrWay.Adapter()).build()
        }

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideConverterFactory(@InternalApi moshi: Moshi): Converter.Factory {
            return MoshiConverterFactory.create(moshi)
        }

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideCallFactory(@InternalApi client: Lazy<OkHttpClient>): Call.Factory {
            return Call.Factory { client.get().newCall(it) }
        }

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideOkHttpClient(@Named("debug") debug: Boolean): OkHttpClient {
            return OkHttpClient.Builder()
                .apply {
                    if (debug) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BODY
                        })
                    }
                }
                .build()
        }

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideRetrofit(
            @InternalApi callFactory: Call.Factory,
            @InternalApi converterFactory: Converter.Factory
        ): Retrofit {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .callFactory(callFactory)
                .addConverterFactory(converterFactory)
                .build()
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideNearbyLocationApi(@InternalApi retrofit: Retrofit): NearbyLocationApi {
            return retrofit.create(NearbyLocationApi::class.java)
        }

        @Provides
        @JvmStatic
        @Singleton
        internal fun provideForegroundHandler(): PermissionHandler<ForegroundLocationPermission> {
            return PermissionHandlerImpl(ForegroundLocationPermission)
        }
    }
}
