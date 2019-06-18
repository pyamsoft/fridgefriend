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

package com.pyamsoft.fridge.locator.map.gms

import android.content.Context
import android.content.Intent
import android.location.Location
import com.google.android.gms.location.LocationResult
import com.pyamsoft.fridge.locator.LocatorBroadcastReceiver
import timber.log.Timber

abstract class GmsLocatorBroadcastReceiver protected constructor() : LocatorBroadcastReceiver() {

  final override fun onLocationUpdate(
    context: Context,
    intent: Intent
  ) {
    val locationResult = LocationResult.extractResult(intent)
    if (locationResult != null) {
      onLocationUpdate(
          context.applicationContext,
          locationResult.lastLocation,
          locationResult.locations
      )
    }
  }

  private fun onLocationUpdate(
    context: Context,
    lastLocation: Location?,
    locations: List<Location>
  ) {
    try {
      inject(context)

      Timber.d("Last location: $lastLocation")
      Timber.d("Other locations: $locations")
    } finally {
      teardown()
    }
  }

  private fun inject(context: Context) {
    onInject(context)
  }

  protected abstract fun onInject(context: Context)

  private fun teardown() {
    onTeardown()
  }

  protected abstract fun onTeardown()

}
