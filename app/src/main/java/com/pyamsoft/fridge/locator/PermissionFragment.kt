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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.locator.map.permission.LocationPermissionScreen
import com.pyamsoft.fridge.locator.map.permission.LocationPermissionViewModel
import com.pyamsoft.fridge.locator.map.permission.PermissionControllerEvent.LocationPermissionRequest
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.app.requireArguments
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

internal class PermissionFragment : Fragment() {

  @JvmField @Inject internal var factory: ViewModelProvider.Factory? = null
  @JvmField @Inject internal var mapPermission: MapPermission? = null

  @JvmField @Inject internal var screen: LocationPermissionScreen? = null
  private val viewModel by factory<LocationPermissionViewModel> { factory }

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

    val parent = view.findViewById<ConstraintLayout>(R.id.layout_constraint)
    Injector.obtain<FridgeComponent>(view.context.applicationContext)
        .plusPermissionComponent()
        .create(parent)
        .inject(this)

    val screen = requireNotNull(screen)

    createComponent(
        savedInstanceState, viewLifecycleOwner,
        viewModel,
        screen
    ) {
      return@createComponent when (it) {
        is LocationPermissionRequest -> requestLocationPermission()
      }
    }

    parent.layout {

      screen.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }
  }

  private fun requestLocationPermission() {
    requireNotNull(mapPermission).requestForegroundPermission(this)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    requireNotNull(mapPermission).onForegroundResult(requestCode, permissions, grantResults) {
      pushMapFragmentOncePermissionGranted()
    }
  }

  private fun pushMapFragmentOncePermissionGranted() {
    val self = this
    requireNotNull(parentFragment).childFragmentManager.commitNow(viewLifecycleOwner) {
      remove(self)
      add(requireArguments().getInt(CONTAINER_ID, 0), MapFragment.newInstance(), MapFragment.TAG)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    screen?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    factory = null
    screen = null
    mapPermission = null
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
