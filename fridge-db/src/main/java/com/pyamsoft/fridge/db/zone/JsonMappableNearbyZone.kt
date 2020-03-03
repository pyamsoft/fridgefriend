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

package com.pyamsoft.fridge.db.zone

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.zone.NearbyZone.Point
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableNearbyZone internal constructor(
    internal val id: NearbyZone.Id,
    internal val name: String,
    internal val createdTime: Date,
    internal val points: List<Point>
) : NearbyZone {

    override fun id(): NearbyZone.Id {
        return id
    }

    override fun points(): List<Point> {
        return points
    }

    override fun name(): String {
        return name
    }

    override fun createdTime(): Date {
        return createdTime
    }

    override fun name(name: String): NearbyZone {
        return this.copy(name = name)
    }

    override fun points(points: List<Point>): NearbyZone {
        return this.copy(points = points)
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun from(item: NearbyZone): JsonMappableNearbyZone {
            return if (item is JsonMappableNearbyZone) item else {
                JsonMappableNearbyZone(
                    item.id(),
                    item.name(),
                    item.createdTime(),
                    item.points()
                )
            }
        }
    }
}
