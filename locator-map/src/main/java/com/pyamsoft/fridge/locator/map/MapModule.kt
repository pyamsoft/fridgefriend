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
import com.pyamsoft.fridge.locator.osm.api.NearbyLocationApi
import com.pyamsoft.fridge.locator.osm.api.OsmNodeOrWay
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
private annotation class InternalApi

@Module
abstract class MapModule {

    @Binds
    @CheckResult
    internal abstract fun bindDeviceGps(impl: GmsLocator): DeviceGps

    @Binds
    @CheckResult
    internal abstract fun bindGeofencer(impl: GmsLocator): Geofencer

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
            return object : Call.Factory {

                override fun newCall(request: Request): Call {
                    return client.get().newCall(request)
                }
            }
        }

        @Provides
        @JvmStatic
        @InternalApi
        internal fun provideOkHttpClient(@Named("debug") debug: Boolean): OkHttpClient {
            return OkHttpClient.Builder()
                .apply {
                    if (debug) {
                        addInterceptor(HttpLoggingInterceptor().apply {
                            level = BODY
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
    }
}
