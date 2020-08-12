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

package com.pyamsoft.fridge.locator.map.osm.popup.store

import android.view.ViewGroup
import com.pyamsoft.fridge.locator.map.osm.popup.base.BaseInfoLocation
import javax.inject.Inject

internal class StoreInfoLocation @Inject internal constructor(
    parent: ViewGroup
) : BaseInfoLocation<StoreInfoViewState, StoreInfoViewEvent>(parent) {

    private fun handlePosition(state: StoreInfoViewState) {
        val position = state.data?.position
        displayLocation(position?.latitude, position?.longitude, state.myLocation, position)
    }

    override fun onRender(state: StoreInfoViewState) {
        handlePosition(state)
    }
}
