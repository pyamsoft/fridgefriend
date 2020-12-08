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

package com.pyamsoft.fridge.locator.map

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.BaseModel
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.fridge.locator.api.NearbyLocationApi
import com.pyamsoft.fridge.locator.api.OsmNodeOrWay.Node
import com.pyamsoft.fridge.locator.api.OsmNodeOrWay.Way
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class MapInteractor @Inject internal constructor(
    private val nearbyStores: NearbyStoreQueryDao,
    private val nearbyZones: NearbyZoneQueryDao,
    private val api: NearbyLocationApi
) {

    @CheckResult
    suspend fun fromCache(): MapMarkers = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        coroutineScope {
            val storeJob = async(context = Dispatchers.Default) { nearbyStores.query(false) }
            val zoneJob = async(context = Dispatchers.Default) { nearbyZones.query(false) }

            val results: List<List<BaseModel<*>>> = awaitAll(storeJob, zoneJob)

            // Unsafe but yeah - what are you gonna do ya know.
            // We want to use awaitAll to run in parallel, so here we are I guess
            @Suppress("UNCHECKED_CAST") val nearbyStores = results[0] as List<NearbyStore>
            @Suppress("UNCHECKED_CAST") val nearbyZones = results[1] as List<NearbyZone>

            return@coroutineScope MapMarkers(nearbyStores, nearbyZones)
        }
    }

    @CheckResult
    suspend fun nearbyLocations(box: BBox): MapMarkers =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()

            Timber.d("Query overpass with bounding box: ${box.south} ${box.west} ${box.north} ${box.east}")
            val data = createOverpassData(box.south, box.west, box.north, box.east)
            val response = api.queryNearby(data)
            val elements = response.elements()

            // First compile all the Way objects
            val polygons = mutableListOf<NearbyZone>()

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

                    polygons.add(NearbyZone.create(way, nodes))
                }

            val remainingNodes = allNodes.filter {
                it.tags.name()
                    .isNotBlank()
            }
            val markers = remainingNodes.map { NearbyStore.create(it) }

            return@withContext MapMarkers(markers, polygons)
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