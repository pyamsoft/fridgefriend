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
import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreDeleteDao
import com.pyamsoft.fridge.db.store.NearbyStoreInsertDao
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.store.NearbyStoreRealtime
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneDeleteDao
import com.pyamsoft.fridge.db.zone.NearbyZoneInsertDao
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZoneRealtime
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoWindow
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoWindow
import com.pyamsoft.fridge.locator.map.osm.updatemanager.LocationUpdatePublisher
import com.pyamsoft.fridge.locator.map.osm.updatemanager.LocationUpdateReceiver
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber
import javax.inject.Inject

class OsmMap @Inject internal constructor(
    private val theming: ThemeProvider,
    private val owner: LifecycleOwner,
    private val imageLoader: ImageLoader,
    private val butler: Butler,

    private val nearbyStoreRealtime: NearbyStoreRealtime,
    private val nearbyStoreQueryDao: NearbyStoreQueryDao,
    private val nearbyStoreInsertDao: NearbyStoreInsertDao,
    private val nearbyStoreDeleteDao: NearbyStoreDeleteDao,

    private val nearbyZoneRealtime: NearbyZoneRealtime,
    private val nearbyZoneQueryDao: NearbyZoneQueryDao,
    private val nearbyZoneInsertDao: NearbyZoneInsertDao,
    private val nearbyZoneDeleteDao: NearbyZoneDeleteDao,

    private val locationUpdateReceiver: LocationUpdateReceiver,
    private val locationUpdatePublisher: LocationUpdatePublisher,
    parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

    override val layout: Int = R.layout.osm_map

    override val layoutRoot by boundView<MapView>(R.id.osm_map)

    private var locationOverlay: MyLocationNewOverlay? = null

    init {
        // Must happen before inflate
        Configuration.getInstance()
            .load(
                parent.context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(parent.context.applicationContext)
            )

        val mapListener = object : MapListener {

            override fun onScroll(event: ScrollEvent): Boolean {
                Timber.d("On map scrolled: $event")
                publishCurrentBoundingBox()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                Timber.d("On map zoomed: $event")
                publishCurrentBoundingBox()
                return true
            }
        }

        doOnInflate {
            owner.lifecycle.addObserver(this)
        }

        doOnInflate {
            initMap(parent.context.applicationContext)
        }

        doOnInflate {
            layoutRoot.setOnDebouncedClickListener { closeAllMapPopups() }
            layoutRoot.addMapListener(mapListener)
            publishCurrentBoundingBox()
        }

        doOnTeardown {
            owner.lifecycle.removeObserver(this)
        }

        doOnTeardown {
            locationOverlay?.let { layoutRoot.overlayManager.remove(it) }
            locationOverlay = null
        }

        doOnTeardown {
            layoutRoot.removeMapListener(mapListener)
            layoutRoot.setOnDebouncedClickListener(null)
            layoutRoot.onDetach()
        }
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
            layoutRoot.invalidate()
        }

        state.requestMapCenter.let { request ->
            if (request != null) {
                locateMe()
            }
        }
    }

    @CheckResult
    private fun renderMapPolygons(zones: List<NearbyZone>): Boolean {
        // Skip work if no polygons
        if (zones.isEmpty()) {
            return false
        }

        val map = layoutRoot
        var changed = false
        val color = Color.argb(75, 255, 255, 0)
        for (zone in zones) {
            val uid = zone.getPolygonUid()
            val oldPolygon = map.overlayManager.filterIsInstance<Polygon>()
                .find { it.id == uid }
            if (oldPolygon != null) {
                // Already have this polygon, avoid extra work
                continue
            }

            // Convert list of nodes to geo points
            val points = ArrayList(zone.points().map { GeoPoint(it.lat, it.lon) })
            // Add the first point again to close the polygon
            points.add(points[0])

            val polygon = Polygon(map).apply {
                infoWindow = ZoneInfoWindow.fromMap(
                    locationUpdateReceiver,
                    zone,
                    map,
                    butler,
                    imageLoader,
                    nearbyZoneRealtime,
                    nearbyZoneQueryDao,
                    nearbyZoneInsertDao,
                    nearbyZoneDeleteDao
                )
                id = uid
                title = zone.name()
                fillPaint.color = color
                outlinePaint.color = if (theming.isDarkTheme()) Color.WHITE else Color.BLACK

                // This sets up the info window location
                setPoints(points)
            }

            polygon.setOnClickListener { p, _, _ ->
                if (p.isInfoWindowOpen) {
                    p.closeInfoWindow()
                } else {
                    p.showInfoWindow()
                }
                return@setOnClickListener true
            }

            map.overlayManager.add(polygon)
            changed = true
        }

        return changed
    }

    @CheckResult
    private fun renderMapMarkers(marks: List<NearbyStore>): Boolean {
        // Skip work if no markers
        if (marks.isEmpty()) {
            return false
        }

        val map = layoutRoot
        var changed = false
        for (mark in marks) {
            val uid = mark.getMarkerUid()
            val oldMarker = map.overlayManager.filterIsInstance<Marker>()
                .find { it.id == uid }
            if (oldMarker != null) {
                // Already have this marker, avoid extra work
                continue
            }

            val marker = Marker(map).apply {
                infoWindow = StoreInfoWindow.fromMap(
                    locationUpdateReceiver,
                    mark,
                    map,
                    butler,
                    imageLoader,
                    nearbyStoreRealtime,
                    nearbyStoreQueryDao,
                    nearbyStoreInsertDao,
                    nearbyStoreDeleteDao
                )
                id = uid
                position = GeoPoint(mark.latitude(), mark.longitude())
                title = mark.name()
            }

            marker.setOnMarkerClickListener { p, _ ->
                if (p.isInfoWindowOpen) {
                    p.closeInfoWindow()
                } else {
                    p.showInfoWindow()
                }
                return@setOnMarkerClickListener true
            }

            map.overlayManager.add(marker)
            changed = true
        }

        return changed
    }

    private fun publishCurrentBoundingBox() {
        publish(OsmViewEvent.UpdateBoundingBox(getBoundingBoxOfCurrentScreen()))
    }

    @CheckResult
    private fun getBoundingBoxOfCurrentScreen(): BBox {
        val mapView = layoutRoot
        val bbox = Projection(
            mapView.zoomLevelDouble, mapView.getIntrinsicScreenRect(null),
            GeoPoint(mapView.mapCenter),
            mapView.mapScrollX, mapView.mapScrollY,
            mapView.mapOrientation,
            mapView.isHorizontalMapRepetitionEnabled, mapView.isVerticalMapRepetitionEnabled,
            MapView.getTileSystem(),
            mapView.mapCenterOffsetX, mapView.mapCenterOffsetY
        ).boundingBox
        return BBox(bbox.latSouth, bbox.lonWest, bbox.latNorth, bbox.lonEast)
    }

    private fun closeAllMapPopups() {
        Timber.d("Closing all open map popups")
        InfoWindow.closeAllInfoWindowsOn(layoutRoot)
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_RESUME)
    internal fun onResume() {

        // Load configuration
        Configuration.getInstance()
            .load(
                layoutRoot.context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(layoutRoot.context.applicationContext)
            )

        layoutRoot.onResume()
    }

    @Suppress("unused")
    @OnLifecycleEvent(ON_PAUSE)
    internal fun onPause() {

        // Save configuration
        Configuration.getInstance()
            .save(
                layoutRoot.context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(layoutRoot.context.applicationContext)
            )

        layoutRoot.onPause()
    }

    private fun initMap(context: Context) {
        layoutRoot.apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setTileSource(TileSourceFactory.MAPNIK)
            addMapOverlays(context)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)

            val filter = if (theming.isDarkTheme()) TilesOverlay.INVERT_COLORS else null
            mapOverlay.setColorFilter(filter)
        }
    }

    private inline fun centerOnLocation(
        // Can't just use location: GeoPoint here because of a kapt missing symbol build-error, fails to compile...
        // what.
        locationProvider: () -> GeoPoint,
        crossinline onCentered: (location: GeoPoint) -> Unit
    ) {
        val location = locationProvider()
        val mapView = layoutRoot
        mapView.post {
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.controller.animateTo(location)
            mapView.controller.setCenter(location)
            onCentered(location)
        }
    }

    private fun locateMe() {
        locationOverlay?.let { overlay ->
            val location = overlay.myLocation
            if (location != null) {
                centerOnLocation(locationProvider = { location }) {
                    Timber.d("Centered onto current user location")
                }
            }
        }
    }

    private fun addMapOverlays(context: Context) {
        val mapView = layoutRoot

        val overlay = UpdateAwareLocationOverlay(
            GpsMyLocationProvider(context),
            mapView,
            onLocationChanged = { locationUpdatePublisher.publish(it) }
        )
        overlay.enableMyLocation()
        mapView.overlayManager.add(overlay)
        locationOverlay = overlay

        overlay.runOnFirstFix {
            publish(OsmViewEvent.RequestMyLocation(automatic = true))
        }
    }

    companion object {

        private const val DEFAULT_ZOOM = 14.8
    }
}
