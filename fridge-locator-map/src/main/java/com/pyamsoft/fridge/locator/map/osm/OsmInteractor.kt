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

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.map.osm.api.NearbyLocationApi
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Node
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Way
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

internal class OsmInteractor @Inject internal constructor(
  private val api: NearbyLocationApi
) {

  @CheckResult
  suspend fun nearbyLocations(box: BBox): List<OsmMarker> = coroutineScope {
    val data = createOverpassData(box.south, box.west, box.north, box.east)
    val response = api.queryNearby(data)
    val elements = response.elements()

    // First compile all the Way objects
    val markers = ArrayList<OsmMarker>()
    val allNodes = elements.filterIsInstance<Node>()
        .toMutableList()
    elements.filterIsInstance<Way>()
        .forEach { way ->
          val nodes = way.nodes.map { id ->
            val node: Node? = allNodes.find { it.id == id }
            if (node == null) {
              return@map null
            } else {
              allNodes.remove(node)
              return@map node
            }
          }
              .filterNot { it == null }
              .map { requireNotNull(it) }

          Timber.d("Way with nodes: $way     $nodes")
          // Now with all our nodes in the way, find the center between all of the nodes and call it the marker
          // for the way.
        }

    val remainingNodes = allNodes.filter {
      it.tags.name()
          .isNotBlank()
    }
    Timber.d("Remaining nodes without Ways: $remainingNodes")
    // For each node, it becomes a marker

    return@coroutineScope markers
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun createOverpassData(
      south: Double,
      west: Double,
      north: Double,
      east: Double
    ): String {
      val box = "$south,$west,$north,$east"
      return """[out:json][timeout:25];(node["shop"="supermarket"]($box);way["shop"="supermarket"]($box);relation["shop"="supermarket"]($box););out body;>;out body qt;"""
    }
  }

}
