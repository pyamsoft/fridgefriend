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
import android.widget.FrameLayout
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.FindNearby
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.popShow
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
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
import javax.inject.Inject

class OsmMap @Inject internal constructor(
  private val owner: LifecycleOwner,
  private val theming: Theming,
  private val imageLoader: ImageLoader,
  activity: Activity,
  parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

  private var markerOverlay: ItemizedOverlayWithFocus<OverlayItem>? = null
  private var activity: Activity? = activity
  private var boundImage: Loaded? = null

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

  override val layoutRoot by boundView<FrameLayout>(R.id.osm_frame)
  private val map by boundView<MapView>(R.id.osm_map)
  private val fab by boundView<FloatingActionButton>(R.id.osm_action)

  init {
    Configuration.getInstance()
        .load(
            activity.application,
            PreferenceManager.getDefaultSharedPreferences(activity.application)
        )
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    owner.lifecycle.addObserver(this)
    initMap(view.context.applicationContext)

    boundImage?.dispose()
    boundImage = imageLoader.load(R.drawable.ic_add_24dp)
        .into(fab)
    fab.isVisible = false
    fab.setOnDebouncedClickListener { publish(FindNearby(getBoundingBoxOfCurrentScreen())) }
  }

  private fun removeMarkerOverlay() {
    markerOverlay?.let { map.overlays.remove(it) }
    markerOverlay = null
  }

  override fun onTeardown() {
    owner.lifecycle.removeObserver(this)
    fab.setOnDebouncedClickListener(null)
    removeMarkerOverlay()
    map.onDetach()
    boundImage?.dispose()
    boundImage = null
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
        ItemizedOverlayWithFocus(
            marks.map { OverlayItem(it.title, it.description, GeoPoint(it.location)) },
            itemListener, map.context.applicationContext
        ).apply {
          setFocusItemsOnTap(true)
        }
            .also { map.overlays.add(it) }
    }
  }

  @CheckResult
  private fun getBoundingBoxOfCurrentScreen(): BBox {
    val mapView = map
    val bbox = Projection(
        mapView.zoomLevelDouble, mapView.getIntrinsicScreenRect(null),
        GeoPoint(mapView.mapCenter),
        mapView.mapScrollX, mapView.mapScrollY,
        mapView.mapOrientation,
        mapView.isHorizontalMapRepetitionEnabled, mapView.isVerticalMapRepetitionEnabled,
        MapView.getTileSystem()
    ).boundingBox
    return BBox(bbox.latSouth, bbox.lonWest, bbox.latNorth, bbox.lonEast)

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
    map.onResume()
  }

  @Suppress("unused")
  @OnLifecycleEvent(ON_PAUSE)
  internal fun onPause() {
    map.onPause()
  }

  private fun initMap(context: Context) {
    map.apply {
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
    val mapView = map

    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), mapView)
    locationOverlay.runOnFirstFix {
      val currentLocation = locationOverlay.myLocation
      mapView.post {
        mapView.controller.setZoom(DEFAULT_ZOOM)
        if (currentLocation != null) {
          mapView.controller.animateTo(currentLocation)
          mapView.controller.setCenter(currentLocation)
          fab.popShow(startDelay = 700L)
        }
      }
    }
    locationOverlay.enableMyLocation()

    val compassOverlay =
      CompassOverlay(context, InternalCompassOrientationProvider(context), mapView)
    compassOverlay.enableCompass()

    mapView.overlays.add(compassOverlay)
    mapView.overlays.add(locationOverlay)
  }

  companion object {

    private const val DEFAULT_ZOOM = 14.5
  }
}
