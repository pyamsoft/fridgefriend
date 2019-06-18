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

package com.pyamsoft.fridge.db.store

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.BaseModel
import com.pyamsoft.fridge.db.IdGenerator
import java.util.Date

interface NearbyStore : BaseModel<NearbyStore> {

  @CheckResult
  fun latitude(): Double

  @CheckResult
  fun longitude(): Double

  companion object {

    const val EMPTY_NAME = ""
    private val EMPTY_CREATED_TIME = Date(0)

    @CheckResult
    fun empty(): NearbyStore {
      return JsonMappableNearbyStore(
          "",
          EMPTY_NAME,
          EMPTY_CREATED_TIME,
          latitude = 0.0,
          longitude = 0.0,
          isArchived = false
      )
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      name: String,
      latitude: Double,
      longitude: Double
    ): NearbyStore {
      return JsonMappableNearbyStore(id, name, Date(), latitude, longitude, isArchived = false)
    }

  }

}
