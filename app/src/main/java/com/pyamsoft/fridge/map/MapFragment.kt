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
import com.pyamsoft.fridge.locator.map.MapControllerEvent
import com.pyamsoft.fridge.locator.map.MapPopupOverlay
import com.pyamsoft.fridge.locator.map.MapViewModel
import com.pyamsoft.fridge.locator.map.osm.OsmActions
import com.pyamsoft.fridge.locator.map.osm.OsmMap
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.R
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import timber.log.Timber
import javax.inject.Inject

internal class MapFragment : Fragment(), SnackbarContainer {

    @JvmField
    @Inject
    internal var map: OsmMap? = null

    @JvmField
    @Inject
    internal var actions: OsmActions? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<MapViewModel>(activity = true) { factory }

    private var stateSaver: StateSaver? = null

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

        val binding = LayoutCoordinatorBinding.bind(view)
        Injector.obtain<FridgeComponent>(view.context.applicationContext)
            .plusMapComponent()
            .create(
                requireActivity(),
                binding.layoutCoordinator,
                viewLifecycleOwner
            )
            .inject(this)


        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            requireNotNull(map),
            requireNotNull(actions)
        ) {
            return@createComponent when (it) {
                is MapControllerEvent.PopupClicked -> revealPopup(it.popup)
            }
        }

        initializeApp()
    }

    private fun revealPopup(popupOverlay: MapPopupOverlay) {
        Timber.d("Show map popup: $popupOverlay")
        popupOverlay.show()
    }

    override fun onStart() {
        super.onStart()
        viewModel.enableGps(requireActivity())
    }

    override fun container(): CoordinatorLayout? {
        return actions?.container()
    }

    private fun initializeApp() {
        val act = requireActivity()
        if (act is VersionChecker) {
            act.checkVersionForUpdate()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()

        factory = null
        map = null
        actions = null
        stateSaver = null
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
