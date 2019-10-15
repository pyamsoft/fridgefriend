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

package com.pyamsoft.fridge.locator.map.osm.popup

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow

internal abstract class BaseInfoWindow protected constructor(
    map: MapView
) : InfoWindow(R.layout.popup_info_layout, map), LifecycleOwner {

    protected val parent = view.findViewById<ConstraintLayout>(R.id.popup_info_root)

    init {
        // A click anywhere but a triggering view will close the popup
        parent.setOnDebouncedClickListener {
            close()
        }
    }

    final override fun onDetach() {
        if (isOpen) {
            close()
        }

        parent.setOnDebouncedClickListener(null)
        onTeardown()
    }

    protected abstract fun onTeardown()
}
