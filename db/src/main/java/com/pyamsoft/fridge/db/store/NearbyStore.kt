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
import com.pyamsoft.fridge.db.BaseModel
import java.util.Date

interface NearbyStore : BaseModel<NearbyStore> {

    @CheckResult
    fun id(): Id

    @CheckResult
    fun latitude(): Double

    @CheckResult
    fun latitude(lat: Double): NearbyStore

    @CheckResult
    fun longitude(): Double

    @CheckResult
    fun longitude(lon: Double): NearbyStore

    data class Id(val id: Long) {

        @CheckResult
        fun isEmpty(): Boolean {
            return id == 0L
        }

        companion object {

            val EMPTY = Id(0)

        }
    }

    companion object {

        @CheckResult
        fun create(
            id: Id,
            name: String,
            createdTime: Date,
            latitude: Double,
            longitude: Double,
        ): NearbyStore {
            return JsonMappableNearbyStore(id, name, createdTime, latitude, longitude)
        }

        @CheckResult
        @JvmOverloads
        fun create(
            store: NearbyStore,
            name: String = store.name(),
            createdTime: Date = store.createdTime(),
            latitude: Double = store.latitude(),
            longitude: Double = store.longitude(),
        ): NearbyStore {
            return JsonMappableNearbyStore(store.id(), name, createdTime, latitude, longitude)
        }
    }
}
