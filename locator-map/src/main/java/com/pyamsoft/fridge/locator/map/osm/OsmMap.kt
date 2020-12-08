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
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
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
    parent: ViewGroup,
) : BaseUiView<MapViewState, MapViewEvent, OsmMapBinding>(parent) {

    override val viewBinding = OsmMapBinding::inflate

    override val layoutRoot by boundView { osmMap }

    private var locationOverlay: MyLocationNewOverlay? = null
    private var centeringLocation = false

    private val mapHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }
    private val boundingBoxHandler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }

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
            binding.osmMap.apply {
                removeMapListener(mapListener)
                onDetach()
            }
        }

        doOnTeardown {
            mapHandler.removeCallbacksAndMessages(null)
            boundingBoxHandler.removeCallbacksAndMessages(null)
        }
    }

    override fun onRender(state: MapViewState) {
        handleMap(state)
        handleMyLocation(state)
    }

    private fun handleMap(state: MapViewState) {
        var invalidate = false
        state.points.let { points ->
            if (renderMapMarkers(points)) {
                invalidate = true
                mapHandler.removeCallbacksAndMessages(null)
            }
        }

        state.zones.let { zones ->
            if (renderMapPolygons(zones)) {
                invalidate = true
                mapHandler.removeCallbacksAndMessages(null)
            }
        }

        if (invalidate) {
            mapHandler.removeCallbacksAndMessages(null)
            mapHandler.postDelayed({
                binding.osmMap.invalidate()
            }, 100)
        }
    }

    @CheckResult
    private fun renderMapPolygons(zones: List<MapViewState.MapZone>): Boolean {
        val map = binding.osmMap
        val overlay = map.overlayManager

        // Remove any polygons that used to exist but don't anymore
        overlay.filterIsInstance<Polygon>()
            .filter { polygon -> zones.find { it.zone.getPolygonUid() == polygon.id } == null }
            .forEach { overlay.remove(it) }

        val color = Color.argb(75, 255, 255, 0)
        return zones.map { zone ->
            val nearby = zone.zone
            val forceOpen = zone.forceOpen

            val uid = nearby.getPolygonUid()
            var polygon = overlay.filterIsInstance<Polygon>().find { it.id == uid }
            if (polygon == null) {
                polygon = Polygon(map).apply {
                    id = uid
                    infoWindow = ZoneInfoWindow.fromMap(
                        locationUpdateReceiver,
                        nearby,
                        map,
                        zoneFactory
                    )
                    fillPaint.color = color
                    outlinePaint.color = if (theming.isDarkTheme()) Color.WHITE else Color.BLACK

                    setOnClickListener { p, _, _ ->
                        publish(MapViewEvent.OpenPopup(p.asPopup()))
                        return@setOnClickListener true
                    }
                }

                overlay.add(polygon)
            } else {
                if (!forceOpen) {
                    // Already have this polygon, avoid extra work
                    return@map false
                }
            }

            // Convert list of nodes to geo points
            val points = ArrayList(nearby.points().map { GeoPoint(it.lat, it.lon) })
            // Add the first point again to close the polygon
            points.add(points[0])

            // This sets up the info window location
            polygon.apply {
                title = nearby.name()
                setPoints(points)
            }

            if (forceOpen) {
                Timber.d("Render zone popup as open: $polygon")
                publish(MapViewEvent.OpenPopup(polygon.asPopup()))
            }

            return@map true
        }.any { it }
    }

    @CheckResult
    private fun renderMapMarkers(marks: List<MapViewState.MapPoint>): Boolean {
        val map = binding.osmMap
        val overlay = map.overlayManager

        // Remove any marks that used to exist but don't anymore
        overlay.filterIsInstance<Marker>()
            .filter { marker -> marks.find { it.point.getMarkerUid() == marker.id } == null }
            .forEach { overlay.remove(it) }

        return marks.map { mark ->
            val nearby = mark.point
            val forceOpen = mark.forceOpen

            val uid = nearby.getMarkerUid()
            var marker = overlay.filterIsInstance<Marker>().find { it.id == uid }
            if (marker == null) {
                // Create a new marker
                marker = Marker(map).apply {
                    id = uid
                    infoWindow = StoreInfoWindow.fromMap(
                        locationUpdateReceiver,
                        nearby,
                        map,
                        storeFactory
                    )

                    setOnMarkerClickListener { p, _ ->
                        publish(MapViewEvent.OpenPopup(p.asPopup()))
                        return@setOnMarkerClickListener true
                    }
                }

                overlay.add(marker)
            } else {
                if (!forceOpen) {
                    // Already have this marker, avoid extra work
                    return@map false
                }
            }

            marker.apply {
                title = nearby.name()
                position = GeoPoint(nearby.latitude(), nearby.longitude())
            }

            if (forceOpen) {
                Timber.d("Render marks popup as open: $marker")
                publish(MapViewEvent.OpenPopup(marker.asPopup()))
            }

            return@map true
        }.any { it }
    }

    private fun publishCurrentBoundingBox() {
        boundingBoxHandler.removeCallbacksAndMessages(null)
        boundingBoxHandler.postDelayed({
            publish(MapViewEvent.UpdateBoundingBox(getBoundingBoxOfCurrentScreen()))
        }, 100)
    }

    @CheckResult
    private fun getBoundingBoxOfCurrentScreen(): BBox {
        val map = binding.osmMap
        val screenBox = Projection(
            map.zoomLevelDouble,
            map.getIntrinsicScreenRect(null),
            GeoPoint(map.mapCenter),
            map.mapScrollX,
            map.mapScrollY,
            map.mapOrientation,
            map.isHorizontalMapRepetitionEnabled,
            map.isVerticalMapRepetitionEnabled,
            MapView.getTileSystem(),
            map.mapCenterOffsetX,
            map.mapCenterOffsetY
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
        crossinline onCentered: (location: GeoPoint) -> Unit,
    ) {
        if (centeringLocation) {
            return
        }

        centeringLocation = true

        val point = GeoPoint(latitude, longitude)
        binding.osmMap.controller.apply {
            setZoom(DEFAULT_ZOOM)
            animateTo(point)
            setCenter(point)
        }
        onCentered(point)

        centeringLocation = false
        publishCurrentBoundingBox()
    }

    private fun addMapOverlays(context: Context) {
        val map = binding.osmMap
        val overlay = UpdateAwareLocationOverlay(GpsMyLocationProvider(context), map) {
            locationUpdatePublisher.publish(it)
        }.apply {
            enableMyLocation()
            runOnFirstFix {
                publish(MapViewEvent.RequestMyLocation(firstTime = true))
            }
        }

        map.overlayManager.add(overlay)
        locationOverlay = overlay
    }

    private fun handleMyLocation(state: MapViewState) {
        state.centerMyLocation?.let {
            // Animation debounce
            if (centeringLocation) {
                return@let
            }

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
