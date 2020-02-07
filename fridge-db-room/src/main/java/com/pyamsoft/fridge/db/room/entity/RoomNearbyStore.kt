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

package com.pyamsoft.fridge.db.room.entity

import androidx.annotation.CheckResult
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pyamsoft.fridge.db.store.NearbyStore
import java.util.Date

@Entity(tableName = RoomNearbyStore.TABLE_NAME)
internal data class RoomNearbyStore internal constructor(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: Long,

    @JvmField
    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @JvmField
    @ColumnInfo(name = COLUMN_CREATED_TIME)
    val createdTime: Date,

    @JvmField
    @ColumnInfo(name = COLUMN_LATITUDE)
    val latitude: Double,

    @JvmField
    @ColumnInfo(name = COLUMN_LONGITUDE)
    val longitude: Double
) : NearbyStore {

    @Ignore
    override fun id(): Long {
        return id
    }

    @Ignore
    override fun name(): String {
        return name
    }

    @Ignore
    override fun createdTime(): Date {
        return createdTime
    }

    @Ignore
    override fun latitude(): Double {
        return latitude
    }

    @Ignore
    override fun longitude(): Double {
        return longitude
    }

    @Ignore
    override fun name(name: String): NearbyStore {
        return NearbyStore.create(this, name = name)
    }

    @Ignore
    override fun latitude(lat: Double): NearbyStore {
        return NearbyStore.create(this, latitude = lat)
    }

    @Ignore
    override fun longitude(lon: Double): NearbyStore {
        return NearbyStore.create(this, longitude = lon)
    }

    companion object {

        @Ignore
        internal const val TABLE_NAME = "room_nearby_store_table"
        @Ignore
        internal const val COLUMN_ID = "_id"
        @Ignore
        internal const val COLUMN_NAME = "name"
        @Ignore
        internal const val COLUMN_CREATED_TIME = "created_time"
        @Ignore
        internal const val COLUMN_LATITUDE = "latitude"
        @Ignore
        internal const val COLUMN_LONGITUDE = "longitude"

        @Ignore
        @JvmStatic
        @CheckResult
        internal fun create(store: NearbyStore): RoomNearbyStore {
            return if (store is RoomNearbyStore) store else {
                RoomNearbyStore(
                    store.id(),
                    store.name(),
                    store.createdTime(),
                    store.latitude(),
                    store.longitude()
                )
            }
        }
    }
}
