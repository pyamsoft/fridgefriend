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
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.injector.ButlerComponent
import com.pyamsoft.fridge.category.CategoryListComponent
import com.pyamsoft.fridge.core.PRIVACY_POLICY_URL
import com.pyamsoft.fridge.core.TERMS_CONDITIONS_URL
import com.pyamsoft.fridge.detail.DetailListComponent
import com.pyamsoft.fridge.detail.expand.ExpandItemCategoryListComponent
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoComponent
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoComponent
import com.pyamsoft.fridge.main.MainActivity
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.bootstrap.libraries.OssLicenses
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class FridgeFriend : Application() {

    @JvmField
    @Inject
    internal var butler: Butler? = null

    private var component: FridgeComponent? = null

    override fun onCreate() {
        super.onCreate()
        val url = "https://github.com/pyamsoft/fridgefriend"
        val parameters = PYDroid.Parameters(
            url,
            "$url/issues",
            PRIVACY_POLICY_URL,
            TERMS_CONDITIONS_URL,
            BuildConfig.VERSION_CODE
        )
        PYDroid.init(this, parameters) { provider ->
            val moshi = Moshi.Builder()
                .build()
            component = DaggerFridgeComponent.factory().create(
                this,
                isDebugMode(),
                moshi,
                provider.theming(),
                provider.enforcer(),
                provider.imageLoader(),
                MainActivity::class.java
            )
            onInitialized()
        }
    }

    private fun onInitialized() {
        addLibraries()
        requireNotNull(component).inject(this)
        beginWork()
    }

    private fun beginWork() {
        GlobalScope.launch(context = Dispatchers.Default) {
            requireNotNull(butler).initOnAppStart(
                ButlerParameters(
                    forceNotifyExpiring = false,
                    forceNotifyNeeded = false
                )
            )
        }
    }

    private fun addLibraries() {
        OssLibraries.add(
            "Room",
            "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/room/",
            "The AndroidX Jetpack Room library. Fluent SQLite database access."
        )
        OssLibraries.add(
            "WorkManager",
            "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/work/",
            "The AndroidX Jetpack WorkManager library. Schedule periodic work in a device friendly way."
        )
        OssLibraries.add(
            "Dagger",
            "https://github.com/google/dagger",
            "A fast dependency injector for Android and Java."
        )
        OssLibraries.add(
            "FastAdapter",
            "https://github.com/mikepenz/fastadapter",
            "The bullet proof, fast and easy to use adapter library, which minimizes developing time to a fraction..."
        )
        OssLibraries.add(
            "OsmDroid",
            "https://github.com/osmdroid/osmdroid",
            "OpenStreetMap-Tools for Android"
        )
        OssLibraries.add(
            "Balloon",
            "https://github.com/skydoves/Balloon",
            "A lightweight popup like tooltips, fully customizable with arrow and animations."
        )
        OssLibraries.add(
            "Google Play Location Services",
            "https://developers.google.com/android/",
            "Google Play Services Location client for Android.",
            license = OssLicenses.custom(
                license = "Custom Google License",
                location = "https://developer.android.com/distribute/play-services"
            )
        )
    }

    override fun getSystemService(name: String): Any? {
        val service = PYDroid.getSystemService(name)
        if (service != null) {
            return service
        }

        return if (name == FridgeComponent::class.java.name) component else {
            getServiceFromComponent(name) ?: super.getSystemService(name)
        }
    }

    @CheckResult
    private fun getServiceFromComponent(name: String): Any? {
        val dependency = provideModuleDependencies(name)
        if (dependency != null) {
            return dependency
        }

        return null
    }

    @CheckResult
    private fun provideModuleDependencies(name: String): Any? {
        return component?.run {
            when (name) {
                ButlerComponent::class.java.name -> plusButlerComponent()
                CategoryListComponent.Factory::class.java.name -> plusCategoryListComponent()
                DetailListComponent.Factory::class.java.name -> plusDetailListComponent()
                ExpandItemCategoryListComponent.Factory::class.java.name -> plusExpandCategoryListComponent()
                StoreInfoComponent.Factory::class.java.name -> plusStoreComponent()
                ZoneInfoComponent.Factory::class.java.name -> plusZoneComponent()
                else -> null
            }
        }
    }
}
