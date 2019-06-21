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
import androidx.annotation.CheckResult
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
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber
import javax.inject.Inject

class OsmMap @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val theming: Theming,
  activity: Activity,
  context: Context,
  parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

  private var markerOverlay: ItemizedOverlayWithFocus<OverlayItem>? = null
  private var activity: Activity? = activity

  private val itemListener = object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {

    override fun onItemLongPress(
      index: Int,
      item: OverlayItem?
    ): Boolean {
      // TODO do something on click
      return true
    }

    override fun onItemSingleTapUp(
      index: Int,
      item: OverlayItem?
    ): Boolean {
      return false
    }

  }

  override val layout: Int = R.layout.osm_map

  override val layoutRoot by boundView<MapView>(R.id.osm_map)

  init {
    Configuration.getInstance()
        .load(context, PreferenceManager.getDefaultSharedPreferences(context))
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    owner.lifecycle.addObserver(this)
    initMap(view.context.applicationContext)
  }

  private fun removeMarkerOverlay() {
    markerOverlay?.let { layoutRoot.overlays.remove(it) }
    markerOverlay = null
  }

  override fun onTeardown() {
    owner.lifecycle.removeObserver(this)
    removeMarkerOverlay()
    layoutRoot.onDetach()
  }

  override fun onRender(
    state: OsmViewState,
    savedState: UiSavedState
  ) {
    removeMarkerOverlay()
    state.markers.let { marks ->
      // Skip work if no markers
      if (marks.isEmpty()) {
        return@let
      }

      markerOverlay =
        ItemizedOverlayWithFocus<OverlayItem>(
            marks.map { OverlayItem(it.title, it.description, GeoPoint(it.location)) },
            itemListener, layoutRoot.context.applicationContext
        ).apply {
          setFocusItemsOnTap(true)
        }
            .also { layoutRoot.overlays.add(it) }
    }
  }

  @CheckResult
  private fun getBoundingBoxOfCurrentScreen(): BoundingBox {
    val mapView = layoutRoot
    return Projection(
        mapView.zoomLevelDouble, mapView.getIntrinsicScreenRect(null),
        GeoPoint(mapView.mapCenter),
        mapView.mapScrollX, mapView.mapScrollY,
        mapView.mapOrientation,
        mapView.isHorizontalMapRepetitionEnabled, mapView.isVerticalMapRepetitionEnabled,
        MapView.getTileSystem()
    ).boundingBox

    // Overpass query to find all supermarkets in the bounding box
    /*
    [out:json][timeout:25];(node["shop"="supermarket"]({{bbox}});way["shop"="supermarket"]({{bbox}});relation["shop"="supermarket"]({{bbox}}););out body;>;out body qt;
     */

    // We get an elements []
    //   If type node
    //     We get an id, lat, lon, timestamp, tags.name
    //      We can build a GeoPoint using lat lng
    //
    //   If type way
    //     We get an id, timestamp, tags.name, nodes (a list of ids)
    //        We search the response body for a node which matches an id in the nodes list
    //        the node which matches id will have a lat lng
    //      We can build a boundingbox using the list of lat lng
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
          mapView.controller.setCenter(currentLocation)
          Timber.d("Bounding box: ${getBoundingBoxOfCurrentScreen()}")
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
