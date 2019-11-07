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

package com.pyamsoft.fridge.locator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.ThemeProvider
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.BackgroundPermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.StoragePermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmMap
import com.pyamsoft.fridge.locator.map.osm.OsmViewModel
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.theme.Theming
import timber.log.Timber
import javax.inject.Inject

internal class MapFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    @JvmField
    @Inject
    internal var map: OsmMap? = null
    @JvmField
    @Inject
    internal var mapPermission: MapPermission? = null
    @JvmField
    @Inject
    internal var deviceGps: DeviceGps? = null
    @JvmField
    @Inject
    internal var theming: Theming? = null
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

        createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            map
        ) {
            return@createComponent when (it) {
                is BackgroundPermissionRequest -> requestBackgroundLocationPermission()
                is StoragePermissionRequest -> requestStoragePermission()
            }
        }

        if (!requireNotNull(mapPermission).hasStoragePermission()) {
            viewModel.requestStoragePermission()
        }

        requireNotNull(deviceGps).enableGps(requireActivity()) {
            Timber.e(it, "Error enabling GPS")
        }
    }

    private fun requestStoragePermission() {
        requireNotNull(mapPermission).requestStoragePermission(
            this,
            onGranted = {
                Timber.d("STORAGE granted")
            },
            onDenied = { Timber.e("STORAGE denied.") })
    }

    private fun requestBackgroundLocationPermission() {
        requireNotNull(mapPermission).requestBackgroundPermission(
            this,
            onGranted = {
                Timber.d("BACKGROUND granted")
            },
            onDenied = { Timber.e("BACKGROUND denied.") })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        map?.saveState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null
        factory = null
        map = null
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
