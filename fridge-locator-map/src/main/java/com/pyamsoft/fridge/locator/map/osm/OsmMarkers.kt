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
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone.Point
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Node
import com.pyamsoft.fridge.locator.map.osm.api.OsmNodeOrWay.Way
import java.util.Date

@CheckResult
internal fun NearbyStore.Companion.create(node: Node): NearbyStore {
  return create(node.id, node.tags.name(), Date(), node.lat, node.lon)
}

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

@CheckResult
internal fun NearbyZone.Companion.create(
  way: Way,
  nodes: List<Node>
): NearbyZone {
  return create(
      way.id,
      findName(way, nodes),
      Date(),
      nodes.map { Point(it.id, it.lat, it.lon) }
  )
}

data class OsmMarkers internal constructor(
  val points: List<NearbyStore>,
  val zones: List<NearbyZone>
)
