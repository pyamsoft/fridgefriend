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

package com.pyamsoft.fridge.map

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.locator.MapPermission
import com.pyamsoft.fridge.locator.R
import com.pyamsoft.fridge.locator.map.osm.OsmActions
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent
import com.pyamsoft.fridge.locator.map.osm.OsmMap
import com.pyamsoft.fridge.locator.map.osm.OsmViewModel
import com.pyamsoft.fridge.locator.permission.BackgroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import com.pyamsoft.fridge.locator.permission.PermissionGrant
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.theme.Theming
import timber.log.Timber
import javax.inject.Inject

internal class MapFragment : Fragment(), SnackbarContainer,
    PermissionConsumer<BackgroundLocationPermission> {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    @JvmField
    @Inject
    internal var map: OsmMap? = null
    @JvmField
    @Inject
    internal var actions: OsmActions? = null
    @JvmField
    @Inject
    internal var mapPermission: MapPermission? = null
    @JvmField
    @Inject
    internal var deviceGps: DeviceGps? = null
    @JvmField
    @Inject
    internal var theming: Theming? = null
    @JvmField
    @Inject
    internal var permissionHandler: PermissionHandler<BackgroundLocationPermission>? =
        null
    private val viewModel by factory<OsmViewModel> { factory }

    private var rootView: ViewGroup? = null

    override fun getSnackbarContainer(): ViewGroup? {
        return rootView
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_coordinator, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val parent = view.findViewById<CoordinatorLayout>(R.id.layout_coordinator)
        rootView = parent
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusMapComponent()
            .create(
                parent,
                viewLifecycleOwner,
                ThemeProvider { requireNotNull(theming).isDarkTheme(requireActivity()) })
            .inject(this)

        val map = requireNotNull(map)
        val actions = requireNotNull(actions)

        createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            map,
            actions
        ) { event ->
            return@createComponent when (event) {
                is OsmControllerEvent.BackgroundPermissionRequest -> requestBackgroundLocationPermission()
                is OsmControllerEvent.MyLocationRequest -> centerMapOnCurrentLocation(event.firstTime)
            }
        }

        requireNotNull(deviceGps).enableGps(requireActivity()) {
            Timber.e(it, "Error enabling GPS")
        }
    }

    private fun centerMapOnCurrentLocation(firstTime: Boolean) {
        requireNotNull(map).findMyLocation()
        requireNotNull(actions).revealButtons(firstTime)
    }

    private fun requestBackgroundLocationPermission() {
        requireNotNull(mapPermission).requestBackgroundPermission(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map?.saveState(outState)
        actions?.saveState(outState)
    }

    override fun onPermissionResponse(grant: PermissionGrant<BackgroundLocationPermission>) {
        if (grant.granted()) {
            Timber.d("Permissions granted: ${grant.permission().permissions()}")
        } else {
            Timber.e("Permissions rejected: ${grant.permission().permissions()}")
        }
    }

    override fun onRequestPermissions(permissions: Array<out String>, requestCode: Int) {
        requestPermissions(permissions, requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requireNotNull(permissionHandler).handlePermissionResponse(
            this, requestCode, permissions, grantResults
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null
        factory = null
        map = null
        actions = null
        mapPermission = null
    }

    companion object {

        const val TAG = "MapFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(): Fragment {
            return MapFragment().apply {
                arguments = Bundle().apply {}
            }
        }
    }
}