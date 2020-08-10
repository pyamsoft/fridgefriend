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

package com.pyamsoft.fridge.db.room.converter

import androidx.annotation.CheckResult
import androidx.room.TypeConverter
import com.pyamsoft.fridge.db.zone.NearbyZone.Point
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

internal object NearbyZonePointListConverter {

    private val adapter by lazy {
        val listOfPointsType = Types.newParameterizedType(List::class.java, Point::class.java)
        return@lazy Moshi.Builder()
            .build()
            .adapter<List<Point>>(listOfPointsType)
    }

    @JvmStatic
    @TypeConverter
    @CheckResult
    fun toPointList(json: String): List<Point> {
        return requireNotNull(adapter.fromJson(json))
    }

    @JvmStatic
    @TypeConverter
    @CheckResult
    fun toString(points: List<Point>): String {
        return adapter.toJson(points)
    }
}
