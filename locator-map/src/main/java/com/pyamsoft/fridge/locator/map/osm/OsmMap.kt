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

package com.pyamsoft.fridge.locator.map.osm

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.location.LocationUpdatePublisher
import com.pyamsoft.fridge.locator.location.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.map.BBox
import com.pyamsoft.fridge.locator.map.MapPopup
import com.pyamsoft.fridge.locator.map.MapViewEvent
import com.pyamsoft.fridge.locator.map.MapViewState
import com.pyamsoft.fridge.locator.map.databinding.OsmMapBinding
import com.pyamsoft.fridge.locator.map.getMarkerUid
import com.pyamsoft.fridge.locator.map.getPolygonUid
import com.pyamsoft.fridge.locator.map.osm.popup.StoreInfoComponent
import com.pyamsoft.fridge.locator.map.osm.popup.StoreInfoWindow
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoComponent
import com.pyamsoft.fridge.locator.map.osm.popup.ZoneInfoWindow
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import org.osmdroid.config.Configuration
import org.osmdroid.config.DefaultConfigurationProvider
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.PolyOverlayWithIW
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
    private val locationUpdateReceiver: LocationUpdateReceiver,
    private val locationUpdatePublisher: LocationUpdatePublisher,
    private val storeFactory: StoreInfoComponent.Factory,
    private val zoneFactory: ZoneInfoComponent.Factory,
    parent: ViewGroup
) : BaseUiView<MapViewState, MapViewEvent, OsmMapBinding>(parent) {

    override val viewBinding = OsmMapBinding::inflate

    override val layoutRoot by boundView { osmMap }

    private var locationOverlay: MyLocationNewOverlay? = null
    private var centeringLocation = false

    init {
        // Must happen before inflate

        // Set the osmdroid output path to our app private directory
        // Apply our configuration as the configuration
        DefaultConfigurationProvider().apply {
            osmdroidBasePath = parent.context.applicationContext.getExternalFilesDir(null)
            Configuration.setConfigurationProvider(this)
        }

        Configuration.getInstance().load(
            parent.context.applicationContext,
            PreferenceManager.getDefaultSharedPreferences(parent.context.applicationContext)
        )

        val mapListener = object : MapListener {

            override fun onScroll(event: ScrollEvent): Boolean {
                publishCurrentBoundingBox()
                return true
            }

            override fun onZoom(event: ZoomEvent): Boolean {
                publishCurrentBoundingBox()
                return true
            }
        }

        doOnInflate {
            val resumeObserver = object : LifecycleObserver {

                @Suppress("unused")
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                fun onResume() {
                    // Load configuration
                    Configuration.getInstance()
                        .load(
                            layoutRoot.context.applicationContext,
                            PreferenceManager.getDefaultSharedPreferences(layoutRoot.context.applicationContext)
                        )

                    binding.osmMap.onResume()
                }
            }

            val pauseObserver = object : LifecycleObserver {

                @Suppress("unused")
                @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                fun onPause() {
                    // Save configuration
                    Configuration.getInstance()
                        .save(
                            layoutRoot.context.applicationContext,
                            PreferenceManager.getDefaultSharedPreferences(layoutRoot.context.applicationContext)
                        )

                    binding.osmMap.onPause()
                }
            }
            owner.lifecycle.apply {
                addObserver(resumeObserver)
                addObserver(pauseObserver)

                doOnTeardown {
                    removeObserver(resumeObserver)
                    removeObserver(pauseObserver)
                }
            }
        }

        doOnInflate {
            initMap(parent.context.applicationContext)
        }

        doOnInflate {
            layoutRoot.setOnDebouncedClickListener { closeAllMapPopups() }
            binding.osmMap.addMapListener(mapListener)
            publishCurrentBoundingBox()
        }

        doOnTeardown {
            locationOverlay?.let { binding.osmMap.overlayManager.remove(it) }
            locationOverlay = null
        }

        doOnTeardown {
            layoutRoot.setOnDebouncedClickListener(null)
            binding.osmMap.removeMapListener(mapListener)
            binding.osmMap.onDetach()
        }
    }

    override fun onRender(state: MapViewState) {
        renderMap(state)
        state.centerMyLocation?.let { findMyLocation() }
    }

    private fun renderMap(state: MapViewState) {
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
            binding.osmMap.invalidate()
        }
    }

    @CheckResult
    private fun renderMapPolygons(zones: List<NearbyZone>): Boolean {
        // Skip work if no polygons
        if (zones.isEmpty()) {
            return false
        }

        var changed = false
        val color = Color.argb(75, 255, 255, 0)
        zones.forEach { zone ->
            val uid = zone.getPolygonUid()
            val oldPolygon = binding.osmMap.overlayManager.filterIsInstance<Polygon>()
                .find { it.id == uid }
            if (oldPolygon != null) {
                // Already have this polygon, avoid extra work
                return@forEach
            }

            // Convert list of nodes to geo points
            val points = ArrayList(zone.points().map { GeoPoint(it.lat, it.lon) })
            // Add the first point again to close the polygon
            points.add(points[0])

            val polygon = Polygon(binding.osmMap).apply {
                infoWindow = ZoneInfoWindow.fromMap(
                    locationUpdateReceiver,
                    zone,
                    binding.osmMap,
                    zoneFactory
                )
                id = uid
                title = zone.name()
                fillPaint.color = color
                outlinePaint.color = if (theming.isDarkTheme()) Color.WHITE else Color.BLACK

                // This sets up the info window location
                setPoints(points)
            }
            polygon.setOnClickListener { p, _, _ ->
                publish(MapViewEvent.OpenPopup(p.asPopup()))
                return@setOnClickListener true
            }

            binding.osmMap.overlayManager.add(polygon)
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

        var changed = false
        marks.forEach { mark ->
            val uid = mark.getMarkerUid()
            val oldMarker = binding.osmMap.overlayManager.filterIsInstance<Marker>()
                .find { it.id == uid }
            if (oldMarker != null) {
                // Already have this marker, avoid extra work
                return@forEach
            }

            val marker = Marker(binding.osmMap).apply {
                infoWindow = StoreInfoWindow.fromMap(
                    locationUpdateReceiver,
                    mark,
                    binding.osmMap,
                    storeFactory
                )
                id = uid
                position = GeoPoint(mark.latitude(), mark.longitude())
                title = mark.name()
            }

            marker.setOnMarkerClickListener { p, _ ->
                publish(MapViewEvent.OpenPopup(p.asPopup()))
                return@setOnMarkerClickListener true
            }

            binding.osmMap.overlayManager.add(marker)
            changed = true
        }

        return changed
    }

    private fun publishCurrentBoundingBox() {
        publish(MapViewEvent.UpdateBoundingBox(getBoundingBoxOfCurrentScreen()))
    }

    @CheckResult
    private fun getBoundingBoxOfCurrentScreen(): BBox {
        val screenBox = Projection(
            binding.osmMap.zoomLevelDouble,
            binding.osmMap.getIntrinsicScreenRect(null),
            GeoPoint(binding.osmMap.mapCenter),
            binding.osmMap.mapScrollX,
            binding.osmMap.mapScrollY,
            binding.osmMap.mapOrientation,
            binding.osmMap.isHorizontalMapRepetitionEnabled,
            binding.osmMap.isVerticalMapRepetitionEnabled,
            MapView.getTileSystem(),
            binding.osmMap.mapCenterOffsetX,
            binding.osmMap.mapCenterOffsetY
        ).boundingBox

        // Always scale the bounding request box to be within screen size - or 20 miles of current location
        // which ever is smaller
        val size = screenBox.diagonalLengthInMeters
        val bbox = if (size <= MAX_ALLOWED_DIAGONAL_LENGTH_METERS) screenBox else {
            val scale = (MAX_ALLOWED_DIAGONAL_LENGTH_METERS / size).toFloat()
            screenBox.increaseByScale(scale)
        }
        return BBox(bbox.latSouth, bbox.lonWest, bbox.latNorth, bbox.lonEast)
    }

    private fun closeAllMapPopups() {
        Timber.d("Closing all open map popups")
        InfoWindow.closeAllInfoWindowsOn(binding.osmMap)
    }

    private fun initMap(context: Context) {
        binding.osmMap.apply {
            setMultiTouchControls(true)
            isTilesScaledToDpi = true
            setTileSource(TileSourceFactory.MAPNIK)
            addMapOverlays(context)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

            val filter = if (theming.isDarkTheme()) TilesOverlay.INVERT_COLORS else null
            mapOverlay.setColorFilter(filter)
        }
    }

    private inline fun centerOnLocation(
        latitude: Double,
        longitude: Double,
        crossinline onCentered: (location: GeoPoint) -> Unit
    ) {
        if (centeringLocation) {
            return
        }

        val point = GeoPoint(latitude, longitude)
        centeringLocation = true
        binding.osmMap.controller.setZoom(DEFAULT_ZOOM)
        binding.osmMap.controller.animateTo(point)
        binding.osmMap.controller.setCenter(point)
        onCentered(point)
        centeringLocation = false
    }

    private fun addMapOverlays(context: Context) {

        val overlay = UpdateAwareLocationOverlay(
            GpsMyLocationProvider(context),
            binding.osmMap,
            onLocationChanged = { locationUpdatePublisher.publish(it) }
        )
        overlay.enableMyLocation()
        binding.osmMap.overlayManager.add(overlay)
        locationOverlay = overlay

        overlay.runOnFirstFix {
            publish(MapViewEvent.RequestMyLocation(firstTime = true))
        }
    }

    private fun findMyLocation() {
        locationOverlay?.let { overlay ->
            val location = overlay.myLocation
            if (location != null) {
                centerOnLocation(location.latitude, location.longitude) {
                    Timber.d("Centered onto current user location")
                    publish(MapViewEvent.DoneFindingMyLocation)
                }
            }
        }
    }

    @CheckResult
    private fun Marker.asPopup(): MapPopup {
        val marker = this
        val position = marker.position
        return object : MapPopup {

            override fun show() {
                closeAllMapPopups()
                centerOnLocation(latitude(), longitude()) {
                    marker.showInfoWindow()
                }
            }

            override fun latitude(): Double {
                return position.latitude
            }

            override fun longitude(): Double {
                return position.longitude
            }
        }
    }

    @CheckResult
    private fun PolyOverlayWithIW.asPopup(): MapPopup {
        val marker = this
        val bounds = marker.bounds
        return object : MapPopup {

            override fun show() {
                closeAllMapPopups()
                centerOnLocation(latitude(), longitude()) {
                    marker.showInfoWindow()
                }
            }

            override fun latitude(): Double {
                return bounds.centerLatitude
            }

            override fun longitude(): Double {
                return bounds.centerLongitude
            }
        }
    }

    companion object {

        private const val DEFAULT_ZOOM = 14.8

        // 20 miles
        private const val MAX_ALLOWED_DIAGONAL_LENGTH_METERS = 32187
    }
}
