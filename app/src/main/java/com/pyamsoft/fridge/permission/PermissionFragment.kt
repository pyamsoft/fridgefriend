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
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.locator.R
import com.pyamsoft.fridge.locator.permission.ForegroundLocationPermission
import com.pyamsoft.fridge.locator.permission.LocationExplanation
import com.pyamsoft.fridge.locator.permission.LocationPermissionViewModel
import com.pyamsoft.fridge.locator.permission.LocationRequestButton
import com.pyamsoft.fridge.locator.permission.PermissionConsumer
import com.pyamsoft.fridge.locator.permission.PermissionControllerEvent.LocationPermissionRequest
import com.pyamsoft.fridge.locator.permission.PermissionGrant
import com.pyamsoft.fridge.locator.permission.PermissionHandler
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.toDp
import javax.inject.Inject
import timber.log.Timber

internal class PermissionFragment : Fragment(), PermissionConsumer<ForegroundLocationPermission> {

    @JvmField
    @Inject
    internal var permissionHandler: PermissionHandler<ForegroundLocationPermission>? = null

    @JvmField
    @Inject
    internal var requestButton: LocationRequestButton? = null

    @JvmField
    @Inject
    internal var explanation: LocationExplanation? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<LocationPermissionViewModel>(activity = true) { factory }

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.layout_constraint, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutConstraintBinding.bind(view)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusPermissionComponent()
            .create(binding.layoutConstraint)
            .inject(this)

        val requestButton = requireNotNull(requestButton)
        val explanation = requireNotNull(explanation)
        stateSaver = createComponent(
            savedInstanceState, viewLifecycleOwner,
            viewModel,
            requestButton,
            explanation
        ) {
            return@createComponent when (it) {
                is LocationPermissionRequest -> requestLocationPermission()
            }
        }

        binding.layoutConstraint.layout {
            requestButton.let {
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )

                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            explanation.let {
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.BOTTOM, requestButton.id(), ConstraintSet.TOP)

                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)

                val hMargin = 16.toDp(view.context)
                val vMargin = 32.toDp(view.context)
                setMargin(it.id(), ConstraintSet.TOP, vMargin)
                setMargin(it.id(), ConstraintSet.BOTTOM, vMargin)
                setMargin(it.id(), ConstraintSet.START, hMargin)
                setMargin(it.id(), ConstraintSet.END, hMargin)
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
        requireActivity().supportFragmentManager.commit(viewLifecycleOwner) {
            replace(
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
        factory = null
        stateSaver = null

        requestButton = null
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
