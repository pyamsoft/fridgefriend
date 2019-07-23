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

package com.pyamsoft.fridge.butler.workmanager.locator

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.BaseWorker
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.pydroid.ui.Injector
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS

internal class LocationWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : BaseWorker(context, params) {

  private var locator: Locator? = null

  override fun onInject() {
    locator = Injector.obtain(applicationContext)
  }

  override fun onTeardown() {
    locator = null
  }

  override fun reschedule(butler: Butler) {
    val loc = requireNotNull(locator)
    if (!loc.hasForegroundPermission()) {
      Timber.w("Missing foreground permission, cannot reschedule.")
      return
    }

    if (!loc.hasBackgroundPermission()) {
      Timber.w("Missing background permission, cannot reschedule.")
      return
    }

    butler.remindLocation(1L, HOURS)
  }

  override suspend fun performWork() {
    Timber.d("LocationWorker listening for updates")

    // There is no corresponding stopListening here
    // the worker is meant to launch and begin the background location
    // listener - not manage the lifecycle
    requireNotNull(locator).listenForUpdates()
  }
}
