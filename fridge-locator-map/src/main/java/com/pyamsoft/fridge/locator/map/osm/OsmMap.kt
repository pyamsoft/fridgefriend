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
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
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
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.FindNearby
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestBackgroundPermission
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.RequestStoragePermission
import com.pyamsoft.fridge.locator.map.osm.popup.LocationUpdateManagerImpl
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoWindow
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoWindow
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
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import timber.log.Timber
import javax.inject.Inject

class OsmMap @Inject internal constructor(
    private val owner: LifecycleOwner,
    private val theming: Theming,
    private val imageLoader: ImageLoader,
    private val butler: Butler,
    private val mapPermission: MapPermission,

    private val nearbyStoreRealtime: NearbyStoreRealtime,
    private val nearbyStoreQueryDao: NearbyStoreQueryDao,
    private val nearbyStoreInsertDao: NearbyStoreInsertDao,
    private val nearbyStoreDeleteDao: NearbyStoreDeleteDao,

    private val nearbyZoneRealtime: NearbyZoneRealtime,
    private val nearbyZoneQueryDao: NearbyZoneQueryDao,
    private val nearbyZoneInsertDao: NearbyZoneInsertDao,
    private val nearbyZoneDeleteDao: NearbyZoneDeleteDao,
    parent: ViewGroup
) : BaseUiView<OsmViewState, OsmViewEvent>(parent), LifecycleObserver {

    override val layout: Int = R.layout.osm_map

    override val layoutRoot by boundView<ViewGroup>(R.id.osm_frame)

    private var boundFindMeImage: Loaded? = null
    private var boundNearbyImage: Loaded? = null
    private var boundStorageImage: Loaded? = null
    private var boundBackgroundImage: Loaded? = null

    private var locationOverlay: MyLocationNewOverlay? = null

    private val map by boundView<MapView>(R.id.osm_map)
    private val findNearby by boundView<FloatingActionButton>(R.id.osm_action)
    private val findMe by boundView<FloatingActionButton>(R.id.osm_find_me)
    private val backgroundPermission by boundView<FloatingActionButton>(
        R.id.osm_background_location_permission
    )
    private val storagePermission by boundView<FloatingActionButton>(R.id.osm_storage_permission)

    private val locationUpdateManager = LocationUpdateManagerImpl()

    init {
        // Must happen before inflate
        Configuration.getInstance()
            .load(
                parent.context.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(parent.context.applicationContext)
            )

        doOnInflate {
            owner.lifecycle.addObserver(this)
            initMap(parent.context.applicationContext)

            boundNearbyImage?.dispose()
            boundNearbyImage = imageLoader.load(R.drawable.ic_shopping_cart_24dp)
                .into(findNearby)

            boundFindMeImage?.dispose()
            boundFindMeImage = imageLoader.load(R.drawable.ic_location_search_24dp)
                .into(findMe)

            boundStorageImage?.dispose()
            boundStorageImage = imageLoader.load(R.drawable.ic_storage_24dp)
                .into(storagePermission)

            boundBackgroundImage?.dispose()
            boundBackgroundImage = imageLoader.load(R.drawable.ic_location_24dp)
                .into(backgroundPermission)

            findNearby.isVisible = false
            findMe.isVisible = false
            storagePermission.isVisible = false
            backgroundPermission.isVisible = false

            findNearby.setOnDebouncedClickListener {
                publish(
                    FindNearby(
                        getBoundingBoxOfCurrentScreen()
                    )
                )
            }
            findMe.setOnDebouncedClickListener { locateMe() }
            backgroundPermission.setOnDebouncedClickListener { publish(RequestBackgroundPermission) }
            storagePermission.setOnDebouncedClickListener { publish(RequestStoragePermission) }
        }

        doOnTeardown {
            owner.lifecycle.removeObserver(this)

            findMe.setOnDebouncedClickListener(null)
            findNearby.setOnDebouncedClickListener(null)
            backgroundPermission.setOnDebouncedClickListener(null)
            storagePermission.setOnDebouncedClickListener(null)

            locationOverlay?.let { map.overlayManager.remove(it) }
            locationOverlay = null
            map.onDetach()

            locationUpdateManager.clear()

            boundFindMeImage?.dispose()
            boundNearbyImage?.dispose()
            boundStorageImage?.dispose()
            boundBackgroundImage?.dispose()
            boundFindMeImage = null
            boundNearbyImage = null
            boundStorageImage = null
            boundBackgroundImage = null
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
                    locationUpdateManager,
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
                fillColor = color
                strokeColor = if (theming.isDarkTheme()) Color.WHITE else Color.BLACK

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
                    locationUpdateManager,
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

            if (theming.isDarkTheme()) {
                mapOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
            }
        }
    }

    private inline fun centerOnLocation(
        // Can't just use location: GeoPoint here because of a kapt missing symbol build-error, fails to compile...
        // what.
        locationProvider: () -> GeoPoint,
        crossinline onCentered: (location: GeoPoint) -> Unit
    ) {
        val location = locationProvider()
        val mapView = map
        mapView.post {
            mapView.controller.setZoom(DEFAULT_ZOOM)
            mapView.controller.animateTo(location)
            mapView.controller.setCenter(location)
            onCentered(location)
        }
    }

    private fun addMapOverlays(context: Context) {
        val mapView = map

        val overlay = UpdateAwareLocationOverlay(
            GpsMyLocationProvider(context),
            mapView,
            onLocationChanged = { locationUpdateManager.publish(it) }
        )

        overlay.runOnFirstFix {
            val currentLocation = overlay.myLocation
            if (currentLocation != null) {
                centerOnLocation(locationProvider = { currentLocation }) {
                    var delay = 700L
                    findNearby.popShow(startDelay = delay)
                    delay += 300L

                    findMe.popShow(startDelay = delay)
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
        overlay.enableMyLocation()
        mapView.overlayManager.add(overlay)
        locationOverlay = overlay
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
