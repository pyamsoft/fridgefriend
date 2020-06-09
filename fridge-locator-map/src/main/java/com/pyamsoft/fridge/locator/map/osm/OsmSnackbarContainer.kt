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
 *
 */

package com.pyamsoft.fridge.locator.map.osm

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.ui.SnackbarContainer
import javax.inject.Inject

class OsmSnackbarContainer @Inject internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup
) : SnackbarContainer<OsmViewState, OsmViewEvent>(owner, parent) {

    override fun onRender(state: OsmViewState) {
        layoutRoot.post { handleNearbyError(state) }
        layoutRoot.post { handleFetchError(state) }
        layoutRoot.post { handleBottomMargin(state) }
    }

    private fun handleBottomMargin(state: OsmViewState) {
        state.bottomOffset.let { height ->
            if (height > 0) {
                addBottomPadding(height)
            }
        }
    }

    private fun handleFetchError(state: OsmViewState) {
        state.cachedFetchError.let { throwable ->
            if (throwable == null) {
                clearCacheError()
            } else {
                showCacheError(throwable)
            }
        }
    }

    private fun handleNearbyError(state: OsmViewState) {
        state.nearbyError.let { throwable ->
            if (throwable == null) {
                clearError()
            } else {
                showError(throwable)
            }
        }
    }

    private fun showError(throwable: Throwable) {
        makeSnackbar("nearby", throwable.message ?: "An unexpected error occurred.")
    }

    private fun clearError() {
        dismissSnackbar("nearby")
    }

    private fun showCacheError(throwable: Throwable) {
        makeSnackbar("cache", throwable.message ?: "An error occurred fetching cached stores.")
    }

    private fun clearCacheError() {
        dismissSnackbar("cache")
    }
}