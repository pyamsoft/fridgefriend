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

package com.pyamsoft.fridge.locator.map.osm.popup

import android.view.ViewGroup
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoContainer.ZoneInfoEvent
import com.pyamsoft.pydroid.arch.EventBus
import java.util.concurrent.atomic.AtomicBoolean

internal abstract class ZoneInfoContainer<T : ZoneInfoEvent> protected constructor(
  private val eventBus: EventBus<T>
) {

  private val inflated = AtomicBoolean(false)

  protected fun publish(event: T) {
    eventBus.publish(event)
  }

  fun inflate(parent: ViewGroup) {
    if (inflated.compareAndSet(false, true)) {
      onInflate(parent)
    }
  }

  protected abstract fun onInflate(parent: ViewGroup)

  fun open(zone: NearbyZone) {
    onOpen(zone)
  }

  protected abstract fun onOpen(zone: NearbyZone)

  fun close() {
    onClose()
  }

  protected abstract fun onClose()

  fun teardown() {
    if (inflated.compareAndSet(true, false)) {
      onTeardown()
    }
  }

  protected abstract fun onTeardown()

  internal sealed class ZoneInfoEvent {

  }
}
