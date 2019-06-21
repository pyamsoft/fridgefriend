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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.ui.theme.Theming
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import javax.inject.Inject

class OsmMap @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val theming: Theming,
  activity: Activity,
  context: Context,
  parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

  private var activity: Activity? = activity

  init {
    setupMapConfiguration(context)
  }

  private fun setupMapConfiguration(context: Context) {
    Configuration.getInstance()
        .load(context, PreferenceManager.getDefaultSharedPreferences(context))
  }

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
      setMultiTouchControls(true)
      isTilesScaledToDpi = true
      setTileSource(TileSourceFactory.MAPNIK)
      addMapOverlays(context)
      zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

      if (theming.isDarkTheme(requireNotNull(activity))) {
        mapOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
      }
      activity = null
    }
  }

  private fun addMapOverlays(context: Context) {
    val mapView = layoutRoot
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), layoutRoot)
    locationOverlay.runOnFirstFix {
      val currentLocation = locationOverlay.myLocation
      mapView.post {
        mapView.controller.setZoom(DEFAULT_ZOOM)
        if (currentLocation != null) {
          mapView.controller.animateTo(currentLocation)
        }
      }
    }
    locationOverlay.enableMyLocation()

    val compassOverlay =
      CompassOverlay(context, InternalCompassOrientationProvider(context), layoutRoot)
    compassOverlay.enableCompass()

    mapView.overlays.add(compassOverlay)
    mapView.overlays.add(locationOverlay)
  }

  companion object {

    private const val DEFAULT_ZOOM = 14.5
  }
}
