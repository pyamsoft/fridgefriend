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

package com.pyamsoft.fridge.locator.osm.updatemanager

import android.location.Location
import android.os.Build
import com.pyamsoft.fridge.locator.osm.updatemanager.LocationUpdateReceiver.Listener
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.LazyThreadSafetyMode.NONE

@Singleton
internal class LocationUpdateManagerImpl @Inject internal constructor() : LocationUpdateReceiver,
    LocationUpdatePublisher {

    private val listeners by lazy(NONE) { mutableSetOf<Listener>() }

    override fun register(listener: Listener) {
        listeners.add(listener)
    }

    override fun unregister(listener: Listener) {
        listeners.remove(listener)
    }

    override fun publish(location: Location?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            listeners.parallelStream().forEach { it.onLocationUpdate(location) }
        } else {
            listeners.forEach { it.onLocationUpdate(location) }
        }
    }
}
