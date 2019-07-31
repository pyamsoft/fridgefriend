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

import android.view.ViewGroup
import android.widget.TextView
import com.pyamsoft.fridge.locator.map.R
import org.osmdroid.views.overlay.OverlayWithIW
import timber.log.Timber
import javax.inject.Inject

internal class ZoneInfoView @Inject internal constructor() : ZoneInfoContainer() {

  private var title: TextView? = null

  override fun onInflate(parent: ViewGroup) {
    Timber.d("onInflate")
    title = parent.findViewById(R.id.zone_info_title)
  }

  override fun onOpen(overlay: OverlayWithIW) {
    Timber.d("onOpen: $overlay")
    requireNotNull(title).text = requireNotNull(overlay.title)
  }

  override fun onClose() {
    Timber.d("onClose")
    requireNotNull(title).text = ""
  }

  override fun onTeardown() {
    Timber.d("onTeardown")
    title = null
  }

}

