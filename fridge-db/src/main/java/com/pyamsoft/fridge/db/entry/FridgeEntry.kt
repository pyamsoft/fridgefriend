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
  fun name(name: String): FridgeEntry

  @CheckResult
  fun createdTime(createdTime: Date): FridgeEntry

  companion object {

    const val EMPTY_NAME = ""
    val EMPTY_CREATED_TIME = Date(0)

    @CheckResult
    fun empty(): FridgeEntry {
      return JsonMappableFridgeEntry("", EMPTY_NAME, EMPTY_CREATED_TIME)
    }

    @CheckResult
    fun create(): FridgeEntry {
      return create(EMPTY_NAME, Date())
    }

    @CheckResult
    fun create(name: String, createdTime: Date): FridgeEntry {
      return JsonMappableFridgeEntry(IdGenerator.generate(), name, createdTime)
    }

    @CheckResult
    fun create(
      entry: FridgeEntry,
      name: String = entry.name(),
      createdTime: Date = entry.createdTime()
    ): FridgeEntry {
      return JsonMappableFridgeEntry(entry.id(), name, createdTime)
    }

  }

}