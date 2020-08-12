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

package com.pyamsoft.fridge.db.zone

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.BaseModel
import com.squareup.moshi.JsonClass
import java.util.Date

interface NearbyZone : BaseModel<NearbyZone> {

    @CheckResult
    fun id(): Id

    @CheckResult
    fun points(): List<Point>

    @CheckResult
    fun points(points: List<Point>): NearbyZone

    @JsonClass(generateAdapter = true)
    // This id needs to be a Long so Moshi can serialize it
    data class Point(
        val id: Long,
        val lat: Double,
        val lon: Double
    )

    data class Id(val id: Long) {

        @CheckResult
        fun isEmpty(): Boolean {
            return id == 0L
        }
    }

    companion object {

        @CheckResult
        fun create(
            id: Id,
            name: String,
            createdTime: Date,
            points: List<Point>
        ): NearbyZone {
            return JsonMappableNearbyZone(id, name, createdTime, points)
        }

        @CheckResult
        @JvmOverloads
        fun create(
            zone: NearbyZone,
            name: String = zone.name(),
            createdTime: Date = zone.createdTime(),
            points: List<Point> = zone.points()
        ): NearbyZone {
            return JsonMappableNearbyZone(zone.id(), name, createdTime, points)
        }
    }
}
