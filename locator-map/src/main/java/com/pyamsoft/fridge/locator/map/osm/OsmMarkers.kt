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
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone.Point
import com.pyamsoft.fridge.locator.osm.api.OsmNodeOrWay.Node
import com.pyamsoft.fridge.locator.osm.api.OsmNodeOrWay.Way

@CheckResult
internal fun NearbyStore.Companion.create(node: Node): NearbyStore {
    return create(NearbyStore.Id(node.id), node.tags.name(), currentDate(), node.lat, node.lon)
}

@CheckResult
internal fun NearbyStore.Companion.getMarkerUidPrefix(): String {
    return requireNotNull(NearbyStore::class.simpleName)
}

@CheckResult
internal fun NearbyStore.getMarkerUid(): String {
    return "${NearbyStore.getMarkerUidPrefix()}${id()}"
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
                break
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
        NearbyZone.Id(way.id),
        findName(way, nodes),
        currentDate(),
        nodes.map { Point(it.id, it.lat, it.lon) }
    )
}

@CheckResult
internal fun NearbyZone.Companion.getPolygonUidPrefix(): String {
    return requireNotNull(NearbyZone::class.simpleName)
}

@CheckResult
internal fun NearbyZone.getPolygonUid(): String {
    return "${NearbyZone.getPolygonUidPrefix()}${id()}"
}

data class OsmMarkers internal constructor(
    val points: List<NearbyStore>,
    val zones: List<NearbyZone>
)
