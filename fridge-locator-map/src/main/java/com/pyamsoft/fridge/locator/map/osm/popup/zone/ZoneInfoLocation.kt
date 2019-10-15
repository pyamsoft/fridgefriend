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

package com.pyamsoft.fridge.locator.map.osm.popup.zone

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.TextView
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.popup.calculateKmDistanceTo
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

internal class ZoneInfoLocation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<ZoneInfoViewState, ZoneInfoViewEvent>(parent) {

    override val layout: Int = R.layout.popup_info_location
    override val layoutRoot by boundView<ViewGroup>(R.id.popup_info_root)

    private val coordinates by boundView<TextView>(R.id.popup_info_coords)
    private val distanceToMe by boundView<TextView>(R.id.popup_info_distance_to_me)

    init {
        doOnTeardown {
            coordinates.text = ""
            distanceToMe.text = ""
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onRender(
        state: ZoneInfoViewState,
        savedState: UiSavedState
    ) {
        state.polygon.let { polygon ->
            if (polygon == null) {
                coordinates.text = ""
            } else {
                val centerPoint = requireNotNull(polygon.infoWindowLocation)
                val lat = "%.5f".format(centerPoint.latitude)
                val lon = "%.5f".format(centerPoint.longitude)
                val coords = "($lat, $lon)"
                coordinates.text = "Located at: $coords"

                val location = state.myLocation
                if (location == null) {
                    distanceToMe.text = ""
                } else {
                    val distance = "%.2f".format(location.calculateKmDistanceTo(centerPoint))
                    distanceToMe.text = "$distance meters away"
                }
            }
        }
    }
}
