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

import android.os.Parcelable
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.BaseModel
import com.squareup.moshi.JsonClass
import kotlinx.android.parcel.Parcelize
import java.util.Date

interface NearbyZone : BaseModel<NearbyZone> {

  @CheckResult
  fun id(): Long

  @CheckResult
  fun points(): List<Point>

  @Parcelize
  @JsonClass(generateAdapter = true)
  data class Point(
    val id: Long,
    val lat: Double,
    val lon: Double
  ) : Parcelable

  companion object {

    const val EMPTY_NAME = ""
    private val EMPTY_CREATED_TIME = Date(0)

    @CheckResult
    fun empty(): NearbyZone {
      return JsonMappableNearbyZone(
          0,
          EMPTY_NAME,
          emptyList(),
          EMPTY_CREATED_TIME,
          isArchived = false
      )
    }

    @CheckResult
    fun create(
      id: Long,
      name: String,
      points: List<Point>
    ): NearbyZone {
      return JsonMappableNearbyZone(id, name, points, Date(), isArchived = false)
    }

  }

}
