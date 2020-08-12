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

package com.pyamsoft.fridge.db.store

import androidx.annotation.CheckResult
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableNearbyStore internal constructor(
    internal val id: NearbyStore.Id,
    internal val name: String,
    internal val createdTime: Date,
    internal val latitude: Double,
    internal val longitude: Double
) : NearbyStore {

    override fun id(): NearbyStore.Id {
        return id
    }

    override fun latitude(): Double {
        return latitude
    }

    override fun longitude(): Double {
        return longitude
    }

    override fun name(): String {
        return name
    }

    override fun createdTime(): Date {
        return createdTime
    }

    override fun name(name: String): NearbyStore {
        return this.copy(name = name.trim())
    }

    override fun latitude(lat: Double): NearbyStore {
        return this.copy(latitude = lat)
    }

    override fun longitude(lon: Double): NearbyStore {
        return this.copy(longitude = lon)
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun from(item: NearbyStore): JsonMappableNearbyStore {
            return if (item is JsonMappableNearbyStore) item else {
                JsonMappableNearbyStore(
                    item.id(),
                    item.name(),
                    item.createdTime(),
                    item.latitude(),
                    item.longitude()
                )
            }
        }
    }
}
