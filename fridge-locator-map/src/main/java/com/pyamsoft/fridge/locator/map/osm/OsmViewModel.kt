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
import com.pyamsoft.fridge.locator.map.osm.OsmViewEvent.FindNearby
import com.pyamsoft.highlander.highlander
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
    initialState = OsmViewState(
        loading = false, points = emptyList(), zones = emptyList(), throwable = null
    )
) {

  private val nearbyRunner = highlander<Unit, BBox> { box ->
    setState { copy(loading = true) }
    try {
      val markers =
        withContext(context = Dispatchers.Default) { interactor.nearbyLocations(box) }
      updateMarkers(markers)
    } catch (e: Throwable) {
      if (e !is CancellationException) {
        Timber.e(e, "Error getting nearby supermarkets")
        setState { copy(throwable = throwable) }
      }
    } finally {
      setState { copy(loading = false) }
    }
  }

  private fun updateMarkers(markers: OsmMarkers) {
    setState {
      copy(
          points = merge(points, markers.points) { it.id() },
          zones = merge(zones, markers.zones) { it.id() }
      )
    }
  }

  private inline fun <T : Any> merge(
    oldList: List<T>,
    newList: List<T>,
    id: (item: T) -> Long
  ): List<T> {
    val result = ArrayList(newList)
    for (oldItem in oldList) {
      val oldId = id(oldItem)
      // If the new list doesn't have the old id, add the old id
      // Otherwise, the new item is newer.
      if (result.find { id(it) == oldId } == null) {
        result.add(oldItem)
      }
    }
    return result
  }

  override fun onInit() {
  }

  override fun handleViewEvent(event: OsmViewEvent) {
    return when (event) {
      is FindNearby -> nearbySupermarkets(event.box)
    }
  }

  private fun nearbySupermarkets(box: BBox) {
    viewModelScope.launch { nearbyRunner.call(box) }
  }

}
