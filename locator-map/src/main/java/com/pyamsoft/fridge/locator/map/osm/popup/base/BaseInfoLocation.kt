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

package com.pyamsoft.fridge.locator.map.osm.popup.base

import android.location.Location
import android.view.ViewGroup
import com.pyamsoft.fridge.locator.map.databinding.PopupInfoLocationBinding
import com.pyamsoft.fridge.locator.map.osm.popup.calculateKmDistanceTo
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import org.osmdroid.util.GeoPoint

internal abstract class BaseInfoLocation<S : UiViewState, E : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BaseUiView<S, E, PopupInfoLocationBinding>(parent) {

    final override val viewBinding = PopupInfoLocationBinding::inflate

    final override val layoutRoot by boundView { popupInfoLocation }

    init {
        doOnTeardown {
            binding.popupInfoCoords.text = ""
            binding.popupInfoDistanceToMe.text = ""
        }
    }

    protected fun displayLocation(
        latitude: Double?,
        longitude: Double?,
        myLocation: Location?,
        centerPoint: GeoPoint?
    ) {
        if (latitude == null || longitude == null) {
            binding.popupInfoCoords.text = ""
        } else {
            val lat = "%.5f".format(latitude)
            val lon = "%.5f".format(longitude)
            val coords = "($lat, $lon)"
            binding.popupInfoCoords.text = "Located at: $coords"

            if (myLocation == null || centerPoint == null) {
                binding.popupInfoDistanceToMe.text = ""
            } else {
                val distance = "%.2f".format(myLocation.calculateKmDistanceTo(centerPoint))
                binding.popupInfoDistanceToMe.text = "${distance}km away"
            }
        }
    }
}
