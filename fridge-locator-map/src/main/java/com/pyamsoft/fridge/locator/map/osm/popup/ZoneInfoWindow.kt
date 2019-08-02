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

import android.view.View
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.DaggerMapViewComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow
import timber.log.Timber
import java.io.Closeable
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

internal class ZoneInfoWindow private constructor(
  private val zone: NearbyZone,
  map: MapView,
  nearbyStoreInsertDao: NearbyStoreInsertDao,
  nearbyStoreDeleteDao: NearbyStoreDeleteDao,
  nearbyZoneInsertDao: NearbyZoneInsertDao,
  nearbyZoneDeleteDao: NearbyZoneDeleteDao
) : InfoWindow(R.layout.zone_info_layout, map) {

  @JvmField @Inject internal var infoView: ZoneInfoView? = null

  private val containerScope: CoroutineScope
    get() = CloseableCoroutineScope(SupervisorJob() + Dispatchers.Main)

  init {
    Timber.d("Dagger inject into ZoneInfoWindow")
    DaggerMapViewComponent.factory()
        .create(
            nearbyStoreInsertDao, nearbyStoreDeleteDao,
            nearbyZoneInsertDao, nearbyZoneDeleteDao
        )
        .inject(this)
  }

  private inline fun allViews(
    required: Boolean,
    withView: (view: ZoneInfoContainer<*>) -> Unit
  ) {
    val views = listOf<ZoneInfoContainer<*>?>(infoView)
    views.forEach { v ->
      if (required) {
        withView(requireNotNull(v))
      } else {
        v?.let(withView)
      }
    }
  }

  override fun onOpen(item: Any?) {
    val v: View? = view
    if (v == null) {
      Timber.e("ZoneInfoWindow.open, mView is null! Bail")
      return
    }

    val layoutRoot = v.findViewById<ConstraintLayout>(R.id.zone_info_root)
    allViews(required = true) { container ->
      container.inflate(layoutRoot)
      container.open(zone)
    }
  }

  override fun onClose() {
    allViews(required = true) { it.close() }
  }

  override fun onDetach() {
    allViews(required = false) { it.teardown() }
    closeCoroutine()
    infoView = null
  }

  private fun closeCoroutine() {
    val scope = containerScope
    if (scope is Closeable) {
      try {
        scope.close()
      } catch (e: Throwable) {
        Timber.e(e, "Failed to close ZoneInfoWindow scope")
      }
    }
  }

  private class CloseableCoroutineScope(context: CoroutineContext) : Closeable, CoroutineScope {
    override val coroutineContext: CoroutineContext = context

    override fun close() {
      coroutineContext.cancel()
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun fromMap(
      zone: NearbyZone,
      map: MapView,
      nearbyStoreInsertDao: NearbyStoreInsertDao,
      nearbyStoreDeleteDao: NearbyStoreDeleteDao,
      nearbyZoneInsertDao: NearbyZoneInsertDao,
      nearbyZoneDeleteDao: NearbyZoneDeleteDao
    ): InfoWindow {
      return ZoneInfoWindow(
          zone, map,
          nearbyStoreInsertDao, nearbyStoreDeleteDao,
          nearbyZoneInsertDao, nearbyZoneDeleteDao
      )
    }
  }
}
