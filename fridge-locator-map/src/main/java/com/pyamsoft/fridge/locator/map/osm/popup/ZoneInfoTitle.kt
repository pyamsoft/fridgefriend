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
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.pydroid.arch.EventBus
import org.osmdroid.views.overlay.OverlayWithIW
import org.osmdroid.views.overlay.Polygon
import javax.inject.Inject

internal class ZoneInfoTitle @Inject internal constructor(
) : ZoneInfoContainer<Nothing>(EventBus.create()) {

  private var title: TextView? = null

  override fun onInflate(parent: ViewGroup) {
    title = parent.findViewById(R.id.zone_info_title)
  }

  override fun onOpen(
    zone: NearbyZone,
      polygon: Polygon
  ) {
    requireNotNull(title).text = zone.name()
  }

  override fun onClose() {
    requireNotNull(title).text = ""
  }

  override fun onTeardown() {
    title = null
  }

}

