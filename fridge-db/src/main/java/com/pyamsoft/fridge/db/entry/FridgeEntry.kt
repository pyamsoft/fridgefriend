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

package com.pyamsoft.fridge.db.entry

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.IdGenerator
import java.util.Date

interface FridgeEntry {

  @CheckResult
  fun id(): String

  @CheckResult
  fun name(): String

  @CheckResult
  fun createdTime(): Date

  @CheckResult
  fun isReal(): Boolean

  @CheckResult
  fun name(name: String): FridgeEntry

  @CheckResult
  fun createdTime(createdTime: Date): FridgeEntry

  @CheckResult
  fun makeReal(): FridgeEntry

  companion object {

    const val EMPTY_NAME = ""
    private val EMPTY_CREATED_TIME = Date(0)

    @CheckResult
    fun empty(): FridgeEntry {
      return JsonMappableFridgeEntry("", EMPTY_NAME, EMPTY_CREATED_TIME, false)
    }

    @CheckResult
    @JvmOverloads
    fun create(id: String = IdGenerator.generate()): FridgeEntry {
      return create(id, EMPTY_NAME, Date(), false)
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      name: String,
      createdTime: Date,
      isReal: Boolean
    ): FridgeEntry {
      return JsonMappableFridgeEntry(id, name, createdTime, isReal)
    }

    @CheckResult
    fun create(
      entry: FridgeEntry,
      name: String = entry.name(),
      createdTime: Date = entry.createdTime(),
      isReal: Boolean
    ): FridgeEntry {
      return JsonMappableFridgeEntry(entry.id(), name, createdTime, isReal)
    }

  }

}