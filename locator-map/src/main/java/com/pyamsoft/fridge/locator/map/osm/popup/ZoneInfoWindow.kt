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

package com.pyamsoft.fridge.locator.map.osm.popup

import android.location.Location
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import com.pyamsoft.fridge.core.createViewModelFactory
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.location.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoLocation
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoTitle
import com.pyamsoft.fridge.locator.map.popup.zone.ZoneInfoViewModel
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.util.layout
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.infowindow.InfoWindow
import javax.inject.Inject
import javax.inject.Provider

class ZoneInfoWindow private constructor(
    receiver: LocationUpdateReceiver,
    zone: NearbyZone,
    map: MapView,
    componentFactory: ZoneInfoComponent.Factory
) : BaseInfoWindow<Polygon>(receiver, map), LifecycleOwner {

    private var stateSaver: StateSaver? = null
    private val lifecycleRegistry by lazy(LazyThreadSafetyMode.NONE) { LifecycleRegistry(this) }

    @JvmField
    @Inject
    internal var infoTitle: ZoneInfoTitle? = null

    @JvmField
    @Inject
    internal var infoLocation: ZoneInfoLocation? = null

    @JvmField
    @Inject
    internal var provider: Provider<ZoneInfoViewModel>? = null
    private val viewModel by fromViewModelFactory<ZoneInfoViewModel>(ViewModelStore()) {
        createViewModelFactory(provider)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    init {
        componentFactory.create(parent, zone).inject(this)

        val title = requireNotNull(infoTitle)
        val location = requireNotNull(infoLocation)

        stateSaver = createComponent(
            null,
            this,
            viewModel,
            title,
            location
        ) {
            // TODO
        }

        parent.layout {
            title.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            location.also {
                connect(it.id(), ConstraintSet.TOP, title.id(), ConstraintSet.BOTTOM)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onPopupOpened(popup: Polygon) {
        val position = popup.bounds
        viewModel.updatePopup(
            name = popup.title,
            latitude = position.centerLatitude,
            longitude = position.centerLongitude
        )
    }

    override fun onLocationUpdate(location: Location?) {
        viewModel.handleLocationUpdate(location)
    }

    override fun onClose() {
    }

    override fun onTeardown() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED

        infoTitle = null
        infoLocation = null
        provider = null
        stateSaver = null
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun fromMap(
            receiver: LocationUpdateReceiver,
            zone: NearbyZone,
            map: MapView,
            factory: ZoneInfoComponent.Factory
        ): InfoWindow {
            return ZoneInfoWindow(receiver, zone, map, factory)
        }
    }
}
