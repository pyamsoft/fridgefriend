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

package com.pyamsoft.fridge.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.locator.R
import com.pyamsoft.fridge.locator.map.permission.LocationPermissionScreen
import com.pyamsoft.fridge.locator.map.permission.LocationPermissionViewModel
import com.pyamsoft.fridge.locator.map.permission.PermissionControllerEvent.LocationPermissionRequest
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import com.pyamsoft.fridge.locator.permission.PermissionGrant
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.pyamsoft.fridge.main.SnackbarContainer
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.commit
import javax.inject.Inject
import timber.log.Timber

internal class PermissionFragment : Fragment(), SnackbarContainer,
    PermissionConsumer<ForegroundLocationPermission> {

    @JvmField
    @Inject
    internal var permissionHandler: PermissionHandler<ForegroundLocationPermission>? = null

    @JvmField
    @Inject
    internal var screen: LocationPermissionScreen? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<LocationPermissionViewModel> { factory }

    private var stateSaver: StateSaver? = null

    private var rootView: CoordinatorLayout? = null

    override fun getSnackbarContainer(): CoordinatorLayout? {
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
            .plusPermissionComponent()
            .create(parent)
            .inject(this)

        val screen = requireNotNull(screen)

        stateSaver = createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            screen
        ) {
            return@createComponent when (it) {
                is LocationPermissionRequest -> requestLocationPermission()
            }
        }
    }

    override fun onRequestPermissions(permissions: Array<out String>, requestCode: Int) {
        requestPermissions(permissions, requestCode)
    }

    override fun onPermissionResponse(grant: PermissionGrant<ForegroundLocationPermission>) {
        if (grant.granted()) {
            pushMapFragmentOncePermissionGranted()
        } else {
            Timber.e("Location permissions denied, cannot show Map")
        }
    }

    private fun requestLocationPermission() {
        viewModel.requestForegroundPermission(this)
    }

    private fun pushMapFragmentOncePermissionGranted() {
        val self = this
        // Do not use commitNow because Assent needs to be the first one in the queue to fire
        requireNotNull(parentFragment).childFragmentManager.commit(viewLifecycleOwner) {
            remove(self)
            add(
                requireArguments().getInt(CONTAINER_ID, 0),
                MapFragment.newInstance(),
                MapFragment.TAG
            )
        }
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

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        rootView = null
        factory = null
        screen = null
        stateSaver = null
    }

    companion object {

        private const val CONTAINER_ID = "parent_container_id"
        const val TAG = "PermissionFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(containerId: Int): Fragment {
            return PermissionFragment().apply {
                arguments = Bundle().apply {
                    putInt(CONTAINER_ID, containerId)
                }
            }
        }
    }
}
