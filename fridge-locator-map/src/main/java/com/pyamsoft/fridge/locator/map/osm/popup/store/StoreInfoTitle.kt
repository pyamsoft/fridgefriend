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
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoTitle
import com.pyamsoft.fridge.locator.map.osm.popup.store.StoreInfoViewEvent.StoreFavoriteAction
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject

internal class StoreInfoTitle @Inject internal constructor(
    private val store: NearbyStore,
    imageLoader: ImageLoader,
    parent: ViewGroup
) : BaseInfoTitle<StoreInfoViewState, StoreInfoViewEvent>(imageLoader, parent, { store.name() }) {

    private fun handleCached(state: StoreInfoViewState) {
        applyFavoriteFromCached(state.cached?.cached)
    }

    override fun onRender(state: StoreInfoViewState) {
        handleCached(state)
    }

    override fun publishFavorite(add: Boolean) {
        publish(StoreFavoriteAction(store, add))
    }
}
