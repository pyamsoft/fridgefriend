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

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.injector.ButlerComponent
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.core.PRIVACY_POLICY_URL
import com.pyamsoft.fridge.core.TERMS_CONDITIONS_URL
import com.pyamsoft.fridge.main.MainActivity
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.ui.ModuleProvider
import com.pyamsoft.pydroid.ui.PYDroid
import com.pyamsoft.pydroid.util.isDebugMode
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class FridgeFriend : Application() {

  @Inject @JvmField internal var butler: Butler? = null

  @Inject @JvmField internal var orderFactory: OrderFactory? = null

  private val component by lazy {
    val url = "https://github.com/pyamsoft/fridgefriend"
    val parameters =
        PYDroid.Parameters(
            url, "$url/issues", PRIVACY_POLICY_URL, TERMS_CONDITIONS_URL, BuildConfig.VERSION_CODE)

    return@lazy createComponent(PYDroid.init(this, parameters))
  }

  private val applicationScope by lazy(LazyThreadSafetyMode.NONE) { MainScope() }

  @CheckResult
  private fun createComponent(provider: ModuleProvider): FridgeComponent {
    return DaggerFridgeComponent.factory()
        .create(
            this,
            isDebugMode(),
            provider.get().theming(),
            provider.get().imageLoader(),
            MainActivity::class.java)
        .also { addLibraries() }
  }

  override fun onCreate() {
    super.onCreate()
    component.inject(this)
    beginWork()
  }

  private fun beginWork() {
    // Coroutine start up is slow. What we can do instead is create a handler, which is cheap, and
    // post
    // to the main thread to defer this work until after start up is done
    Handler(Looper.getMainLooper()).post {
      applicationScope.launch(context = Dispatchers.Default) {
        requireNotNull(butler).initOnAppStart(requireNotNull(orderFactory))
      }
    }
  }

  override fun getSystemService(name: String): Any? {
    // Use component here in a weird way to guarantee the lazy is initialized.
    return component.run { PYDroid.getSystemService(name) } ?: fallbackGetSystemService(name)
  }

  @CheckResult
  private fun fallbackGetSystemService(name: String): Any? {
    return if (name == FridgeComponent::class.java.name) component
    else {
      provideModuleDependencies(name) ?: super.getSystemService(name)
    }
  }

  @CheckResult
  private fun provideModuleDependencies(name: String): Any? {
    return component.run {
      when (name) {
        ButlerComponent::class.java.name -> plusButlerComponent()
        else -> null
      }
    }
  }

  companion object {

    @JvmStatic
    private fun addLibraries() {
      // We are using pydroid-notify
      OssLibraries.usingNotify = true

      // We are using pydroid-autopsy
      OssLibraries.usingAutopsy = true

      OssLibraries.add(
          "Room",
          "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/room/",
          "The AndroidX Jetpack Room library. Fluent SQLite database access.")
      OssLibraries.add(
          "WorkManager",
          "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/work/",
          "The AndroidX Jetpack WorkManager library. Schedule periodic work in a device friendly way.")
      OssLibraries.add(
          "Dagger",
          "https://github.com/google/dagger",
          "A fast dependency injector for Android and Java.")
      OssLibraries.add(
          "FastAdapter",
          "https://github.com/mikepenz/fastadapter",
          "The bullet proof, fast and easy to use adapter library, which minimizes developing time to a fraction...")
      OssLibraries.add(
          "Balloon",
          "https://github.com/skydoves/Balloon",
          "A lightweight popup like tooltips, fully customizable with arrow and animations.")
    }
  }
}
