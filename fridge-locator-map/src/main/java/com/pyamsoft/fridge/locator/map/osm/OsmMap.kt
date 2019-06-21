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

package com.pyamsoft.fridge.locator.map.osm

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gridlines.LatLonGridlineOverlay2
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject

class OsmMap @Inject internal constructor(
  private val owner: LifecycleOwner,
  parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

  override val layout: Int = R.layout.osm_map

  override val layoutRoot by boundView<MapView>(R.id.osm_map)

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    owner.lifecycle.addObserver(this)
    initMap(view.context.applicationContext)
  }

  override fun onTeardown() {
    owner.lifecycle.removeObserver(this)
    layoutRoot.onDetach()
  }

  override fun onRender(
    state: OsmViewState,
    savedState: UiSavedState
  ) {
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_RESUME)
  internal fun onResume() {
    layoutRoot.onResume()
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_PAUSE)
  internal fun onPause() {
    layoutRoot.onPause()
  }

  private fun initMap(context: Context) {
    layoutRoot.apply {
      addMapOverlays(context, overlays)
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
      setMultiTouchControls(true)
    }
  }

  private fun addMapOverlays(
    context: Context,
    overlays: MutableList<Overlay>
  ) {
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), layoutRoot)
    locationOverlay.enableMyLocation()

    val compassOverlay =
      CompassOverlay(context, InternalCompassOrientationProvider(context), layoutRoot)
    compassOverlay.enableCompass()

    overlays.add(locationOverlay)
    overlays.add(compassOverlay)
    overlays.add(LatLonGridlineOverlay2())
  }
}
