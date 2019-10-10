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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import android.view.ViewGroup
import android.widget.TextView
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

internal class StoreInfoLocation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<StoreInfoViewState, StoreInfoViewEvent>(parent) {

    override val layout: Int = R.layout.zone_info_location
    override val layoutRoot by boundView<TextView>(R.id.zone_info_coords)

    init {
        doOnTeardown {
            layoutRoot.text = ""
        }
    }

    override fun onRender(
        state: StoreInfoViewState,
        savedState: UiSavedState
    ) {
        state.marker.let { marker ->
            if (marker == null) {
                layoutRoot.text = ""
            } else {
                val lat = "%.5f".format(marker.position.latitude)
                val lon = "%.5f".format(marker.position.longitude)
                val coords = "($lat, $lon)"
                layoutRoot.text = "Located at: $coords"
            }
        }
    }
}