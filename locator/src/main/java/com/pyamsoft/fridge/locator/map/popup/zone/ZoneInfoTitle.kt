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

package com.pyamsoft.fridge.locator.map.popup.zone

import android.view.ViewGroup
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.popup.base.BaseInfoTitle
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject

class ZoneInfoTitle @Inject internal constructor(
    private val zone: NearbyZone,
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseInfoTitle<ZoneInfoViewState, ZoneInfoViewEvent>(imageLoader, parent) {

    override fun publishFavorite(add: Boolean) {
        publish(ZoneInfoViewEvent.ZoneFavoriteAction(zone, add))
    }
}
