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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.osmdroid.views.overlay.OverlayWithIW
import timber.log.Timber
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

internal abstract class ZoneInfoContainer {

  val containerScope: CoroutineScope
    get() = CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main)

  private val inflated = AtomicBoolean(false)

  fun inflate(parent: ViewGroup) {
    if (inflated.compareAndSet(false, true)) {
      onInflate(parent)
    }
  }

  protected abstract fun onInflate(parent: ViewGroup)

  fun open(overlay: OverlayWithIW) {
    onOpen(overlay)
  }

  protected abstract fun onOpen(overlay: OverlayWithIW)

  fun close() {
    onClose()
  }

  protected abstract fun onClose()

  fun teardown() {
    closeCoroutine()
    if (inflated.compareAndSet(true, false)) {
      onTeardown()
    }
  }

  private fun closeCoroutine() {
    val scope = containerScope
    if (scope is Closeable) {
      try {
        scope.close()
      } catch (e: Throwable) {
        Timber.e(e, "Failed to close ZoneInfoContainer scope")
      }
    }
  }

  protected abstract fun onTeardown()

  private class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
      coroutineContext.cancel()
    }
  }
}
