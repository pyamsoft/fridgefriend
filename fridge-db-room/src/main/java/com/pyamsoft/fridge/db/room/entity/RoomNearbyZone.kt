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
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZone.Point
import java.util.Date

@Entity(tableName = RoomNearbyZone.TABLE_NAME)
internal data class RoomNearbyZone internal constructor(
    @JvmField
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: NearbyZone.Id,

    @JvmField
    @ColumnInfo(name = COLUMN_NAME)
    val name: String,

    @JvmField
    @ColumnInfo(name = COLUMN_CREATED_TIME)
    val createdTime: Date,

    @JvmField
    @ColumnInfo(name = COLUMN_POINTS)
    val points: List<Point>
) : NearbyZone {

    @Ignore
    override fun id(): NearbyZone.Id {
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
    override fun points(): List<Point> {
        return points
    }

    @Ignore
    override fun name(name: String): NearbyZone {
        return NearbyZone.create(this, name = name)
    }

    @Ignore
    override fun points(points: List<Point>): NearbyZone {
        return NearbyZone.create(this, points = points)
    }

    companion object {

        @Ignore
        internal const val TABLE_NAME = "room_nearby_zone_table"
        @Ignore
        internal const val COLUMN_ID = "_id"
        @Ignore
        internal const val COLUMN_NAME = "name"
        @Ignore
        internal const val COLUMN_CREATED_TIME = "created_time"
        @Ignore
        internal const val COLUMN_POINTS = "points"

        @Ignore
        @JvmStatic
        @CheckResult
        internal fun create(zone: NearbyZone): RoomNearbyZone {
            return if (zone is RoomNearbyZone) zone else {
                RoomNearbyZone(
                    zone.id(),
                    zone.name(),
                    zone.createdTime(),
                    zone.points()
                )
            }
        }
    }
}
