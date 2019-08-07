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

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.R
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoViewEvent.ZoneFavoriteAction
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.Loaded
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject

internal class ZoneInfoTitle @Inject internal constructor(
  private val zone: NearbyZone,
  private val imageLoader: ImageLoader,
  parent: ViewGroup
) : BaseUiView<ZoneInfoViewState, ZoneInfoViewEvent>(parent) {

  override val layout: Int = R.layout.zone_info_title
  override val layoutRoot by boundView<ViewGroup>(R.id.zone_info_title_root)
  private val title by boundView<TextView>(R.id.zone_info_title)
  private val favorite by boundView<ImageView>(R.id.zone_info_favorite)

  private var favoriteBinder: Loaded? = null

  private fun clearFavoriteIcon() {
    favoriteBinder?.dispose()
    favoriteBinder = null
  }

  override fun onInflated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    title.text = zone.name()
  }

  override fun onRender(
    state: ZoneInfoViewState,
    savedState: UiSavedState
  ) {
    state.cached.let { cached ->
      if (cached == null) {
        favorite.setOnDebouncedClickListener(null)
      } else {
        val icon = if (cached.cached) R.drawable.ic_star_24dp else R.drawable.ic_star_empty_24dp
        clearFavoriteIcon()
        favoriteBinder = imageLoader.load(icon)
            .into(favorite)
        favorite.setOnDebouncedClickListener {
          publish(ZoneFavoriteAction(zone, !cached.cached))
        }
      }
    }
  }

  override fun onTeardown() {
    title.text = ""
    favorite.setOnDebouncedClickListener(null)
    clearFavoriteIcon()
  }

}

