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
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.FindNearby
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestBackgroundPermission
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestStoragePermission
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoWindow
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.theme.Theming
import com.pyamsoft.pydroid.ui.util.Snackbreak
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
import org.osmdroid.views.overlay.Polygon
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
  private val mapPermission: MapPermission,
  private val nearbyStoreInsertDao: NearbyStoreInsertDao,
  private val nearbyStoreDeleteDao: NearbyStoreDeleteDao,
  private val nearbyZoneInsertDao: NearbyZoneInsertDao,
  private val nearbyZoneDeleteDao: NearbyZoneDeleteDao,
  activity: Activity,
  parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

  override val layout: Int = R.layout.osm_map

  override val layoutRoot by boundView<ViewGroup>(R.id.osm_frame)

  private var markerOverlay: ItemizedOverlayWithFocus<OverlayItem>? = null
  private var activity: Activity? = activity
  private var boundNearbyImage: Loaded? = null
  private var boundStorageImage: Loaded? = null
  private var boundBackgroundImage: Loaded? = null

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

  private val map by boundView<MapView>(R.id.osm_map)
  private val findNearby by boundView<FloatingActionButton>(R.id.osm_action)
  private val backgroundPermission by boundView<FloatingActionButton>(
      R.id.osm_background_location_permission
  )
  private val storagePermission by boundView<FloatingActionButton>(R.id.osm_storage_permission)

  init {
    // Must happen before inflate
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

    boundNearbyImage?.dispose()
    boundNearbyImage = imageLoader.load(R.drawable.ic_location_search_24dp)
        .into(findNearby)

    boundStorageImage?.dispose()
    boundStorageImage = imageLoader.load(R.drawable.ic_storage_24dp)
        .into(storagePermission)

    boundBackgroundImage?.dispose()
    boundBackgroundImage = imageLoader.load(R.drawable.ic_location_24dp)
        .into(backgroundPermission)

    findNearby.isVisible = false
    storagePermission.isVisible = false
    backgroundPermission.isVisible = false

    findNearby.setOnDebouncedClickListener { publish(FindNearby(getBoundingBoxOfCurrentScreen())) }
    backgroundPermission.setOnDebouncedClickListener { publish(RequestBackgroundPermission) }
    storagePermission.setOnDebouncedClickListener { publish(RequestStoragePermission) }
  }

  override fun onTeardown() {
    owner.lifecycle.removeObserver(this)
    findNearby.setOnDebouncedClickListener(null)
    backgroundPermission.setOnDebouncedClickListener(null)
    storagePermission.setOnDebouncedClickListener(null)
    removeMarkerOverlay()
    map.onDetach()

    boundNearbyImage?.dispose()
    boundStorageImage?.dispose()
    boundBackgroundImage?.dispose()
    boundNearbyImage = null
    boundStorageImage = null
    boundBackgroundImage = null
  }

  private fun removeMarkerOverlay() {
    markerOverlay?.let { map.overlays.remove(it) }
    markerOverlay = null
  }

  override fun onRender(
    state: OsmViewState,
    savedState: UiSavedState
  ) {
    var invalidate = false
    state.points.let { points ->
      if (renderMapMarkers(points)) {
        invalidate = true
      }
    }

    state.zones.let { zones ->
      if (renderMapPolygons(zones)) {
        invalidate = true
      }
    }

    if (invalidate) {
      map.invalidate()
    }

    state.nearbyError.let { throwable ->
      if (throwable == null) {
        clearError()
      } else {
        showError(throwable)
      }
    }

    state.cachedFetchError.let { throwable ->
      if (throwable == null) {
        clearCacheError()
      } else {
        showCacheError(throwable)
      }
    }
  }

  @CheckResult
  private fun renderMapPolygons(zones: List<NearbyZone>): Boolean {
    // Skip work if no polygons
    if (zones.isEmpty()) {
      return false
    }

    val color = Color.argb(75, 255, 255, 0)
    for (zone in zones) {
      // Convert list of nodes to geo points
      val points = ArrayList(zone.points().map { GeoPoint(it.lat, it.lon) })
      // Add the first point again to close the polygon
      points.add(points[0])

      val uid = "OsmPolygon: ${zone.id()}"
      val polygon = Polygon(map).apply {
        infoWindow = ZoneInfoWindow.fromMap(
            map,
            nearbyStoreInsertDao, nearbyStoreDeleteDao,
            nearbyZoneInsertDao, nearbyZoneDeleteDao
        )
        setPoints(points)
        fillColor = color
        title = zone.name()
        id = uid
      }


      polygon.setOnClickListener { p, _, _ ->
        if (p.isInfoWindowOpen) {
          p.closeInfoWindow()
        } else {
          p.showInfoWindow()
        }
        return@setOnClickListener true
      }

      val oldPolygon = map.overlayManager.filterIsInstance<Polygon>()
          .find { it.id == uid }
      if (oldPolygon != null) {
        map.overlayManager.remove(oldPolygon)
      }
      map.overlayManager.add(polygon)
    }
    return true
  }

  @CheckResult
  private fun renderMapMarkers(marks: List<NearbyStore>): Boolean {
    // Skip work if no markers
    if (marks.isEmpty()) {
      return false
    }

    markerOverlay?.let { overlay ->
      // Clear old overlay if it exists
      overlay.removeAllItems(false)

      // Remove old overlay from map
      map.overlays.remove(overlay)
    }

    markerOverlay =
      ItemizedOverlayWithFocus(
          marks.map { point ->
            val name = point.name()
            val description = "Supermarket: $name"
            val geo = GeoPoint(point.latitude(), point.longitude())
            val uid = "OsmGeoPoint: ${point.id()}"
            return@map OverlayItem(uid, name, description, geo)
          },
          itemListener, map.context.applicationContext
      ).apply {
        setFocusItemsOnTap(true)
      }
          .also { map.overlays.add(it) }

    return true
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

          var delay = 700L
          findNearby.popShow(startDelay = delay)
          delay += 300L

          storagePermission.popShow(startDelay = delay)
          delay += 300L

          if (!mapPermission.hasBackgroundPermission()) {
            backgroundPermission.popShow(startDelay = delay)
            delay += 300L
          }
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

  private fun showError(throwable: Throwable) {
    Snackbreak.bindTo(owner, "nearby") {
      make(layoutRoot, throwable.message ?: "An unexpected error occurred.")
    }
  }

  private fun clearError() {
    Snackbreak.bindTo(owner, "nearby") {
      dismiss()
    }
  }

  private fun showCacheError(throwable: Throwable) {
    Snackbreak.bindTo(owner, "cache") {
      make(layoutRoot, throwable.message ?: "An error occurred fetching cached stores.")
    }
  }

  private fun clearCacheError() {
    Snackbreak.bindTo(owner, "cache") {
      dismiss()
    }
  }

  companion object {

    private const val DEFAULT_ZOOM = 14.8
  }
}
