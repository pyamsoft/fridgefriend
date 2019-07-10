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

package com.pyamsoft.fridge.locator.map.osm

import androidx.lifecycle.viewModelScope
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class OsmViewModel @Inject internal constructor(
  private val interactor: OsmInteractor
) : UiViewModel<OsmViewState, OsmViewEvent, OsmControllerEvent>(
    initialState = OsmViewState(markers = emptyList())
) {

  override fun onInit() {
  }

  override fun handleViewEvent(event: OsmViewEvent) {
    // TODO
  }

  private fun nearbySupermarkets(box: BBox) {
    viewModelScope.launch {
      // TODO setState begin
      try {
        val response =
          withContext(context = Dispatchers.Default) { interactor.nearbyLocations(box) }
        // TODO setState success
      } catch (e: Throwable) {
        if (e !is CancellationException) {
          Timber.e(e, "Error getting nearby supermarkets")
          // TODO setState error
        }
      } finally {
        // TODO setState complete
      }
    }
  }

}
