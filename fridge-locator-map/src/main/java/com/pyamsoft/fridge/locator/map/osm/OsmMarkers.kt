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
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Node
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Way

data class OsmGeoPoint internal constructor(
  val id: Long,
  val lat: Double,
  val lon: Double,
  val name: String
) {

  companion object {

    @JvmStatic
    @CheckResult
    internal fun create(node: Node): OsmGeoPoint {
      return OsmGeoPoint(node.id, node.lat, node.lon, node.tags.name())
    }
  }
}

data class OsmPoint internal constructor(
  val id: Long,
  val lat: Double,
  val lon: Double
)

data class OsmPolygon internal constructor(
  val id: Long,
  val name: String,
  val nodes: List<OsmPoint>
) {
  companion object {

    @JvmStatic
    @CheckResult
    private fun findName(
      way: Way,
      nodes: List<Node>
    ): String {
      var name = ""
      val wayName = way.tags.name()
      if (wayName.isNotBlank()) {
        name = wayName
      } else {
        for (node in nodes) {
          val nodeName = node.tags.name()
          if (nodeName.isNotBlank()) {
            name = nodeName
          }
        }
      }

      return name
    }

    @JvmStatic
    @CheckResult
    internal fun create(
      way: Way,
      nodes: List<Node>
    ): OsmPolygon {
      return OsmPolygon(way.id, findName(way, nodes), nodes.map { OsmPoint(it.id, it.lat, it.lon) })
    }
  }
}

data class OsmMarkers internal constructor(
  val polygons: List<OsmPolygon>,
  val markers: List<OsmGeoPoint>
) {
  companion object {

    @JvmStatic
    @CheckResult
    internal fun empty(): OsmMarkers {
      return OsmMarkers(emptyList(), emptyList())
    }
  }
}
