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
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.BackgroundPermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmControllerEvent.StoragePermissionRequest
import com.pyamsoft.fridge.locator.map.osm.OsmMap
import com.pyamsoft.fridge.locator.map.osm.OsmViewModel
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.util.layout
import timber.log.Timber
import javax.inject.Inject

internal class MapFragment : Fragment() {

  @JvmField @Inject internal var factory: ViewModelProvider.Factory? = null
  @JvmField @Inject internal var map: OsmMap? = null
  @JvmField @Inject internal var mapPermission: MapPermission? = null
  private val viewModel by factory<OsmViewModel> { factory }

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
        .plusMapComponent()
        .create(requireActivity(), parent, viewLifecycleOwner)
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

    parent.layout {

      map.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }

    if (!requireNotNull(mapPermission).hasStoragePermission()) {
      viewModel.requestStoragePermission()
    }
  }

  private fun requestStoragePermission() {
    requireNotNull(mapPermission).requestStoragePermission(this)
  }

  private fun requestBackgroundLocationPermission() {
    requireNotNull(mapPermission).requestBackgroundPermission(this)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<out String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    requireNotNull(mapPermission).let { mp ->
      mp.onBackgroundResult(requestCode, permissions, grantResults) {
        Timber.d("BACKGROUND permission granted!")
        // TODO Handle background permission granted state
      }

      mp.onStorageResult(requestCode, permissions, grantResults) {
        Timber.d("STORAGE permission granted!")
        // TODO Handle storage permission granted state
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    map?.saveState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

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
