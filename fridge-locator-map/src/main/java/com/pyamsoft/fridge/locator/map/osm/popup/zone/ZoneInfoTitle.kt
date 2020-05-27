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

import android.view.ViewGroup
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoTitle
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoViewEvent.ZoneFavoriteAction
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject

internal class ZoneInfoTitle @Inject internal constructor(
    private val zone: NearbyZone,
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseInfoTitle<ZoneInfoViewState, ZoneInfoViewEvent>(imageLoader, parent, { zone.name() }) {

    init {
        doOnTeardown {
            layoutRoot.handler?.removeCallbacksAndMessages(null)
        }
    }

    override fun onRender(state: ZoneInfoViewState) {
        layoutRoot.post { applyFavoriteFromCached(cached = state.cached?.cached) }
    }

    override fun publishFavorite(add: Boolean) {
        publish(ZoneFavoriteAction(zone, add))
    }
}
