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

package com.pyamsoft.fridge.locator.map.osm.popup

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.locator.location.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.databinding.PopupInfoLayoutBinding
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.infowindow.InfoWindow
import timber.log.Timber

abstract class BaseInfoWindow<T : Any> protected constructor(
    private val receiver: LocationUpdateReceiver,
    map: MapView
) : InfoWindow(R.layout.popup_info_layout, map), LifecycleOwner, LocationUpdateReceiver.Listener {

    protected val parent: ConstraintLayout

    init {
        val binding = PopupInfoLayoutBinding.bind(view)
        parent = binding.popupInfoRoot

        // A click anywhere but a triggering view will close the popup
        parent.setOnDebouncedClickListener {
            close()
        }

        listenForLocationUpdates()
    }

    private fun listenForLocationUpdates() {
        receiver.register(this)
    }

    final override fun onOpen(item: Any?) {
        val v: View? = view
        if (v == null) {
            Timber.e("BaseInfoWindow.open, mView is null! Bail")
            return
        }

        if (item == null) {
            Timber.e("BaseInfoWindow.open, item is null! Bail")
            return
        }

        @Suppress("UNCHECKED_CAST") val popup = item as? T
        if (popup == null) {
            Timber.e("BaseInfoWindow.open, item is not popup type! Bail")
            return
        }

        onPopupOpened(popup)
    }

    protected abstract fun onPopupOpened(popup: T)

    final override fun onDetach() {
        if (isOpen) {
            close()
        }

        receiver.unregister(this)
        parent.setOnDebouncedClickListener(null)
        onTeardown()
    }

    protected abstract fun onTeardown()
}
