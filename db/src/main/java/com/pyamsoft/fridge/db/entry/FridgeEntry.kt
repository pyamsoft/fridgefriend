/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.db.entry

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.core.IdGenerator
import com.pyamsoft.fridge.core.currentDate
import com.pyamsoft.fridge.db.BaseModel
import java.util.Date

interface FridgeEntry : BaseModel<FridgeEntry> {

  @CheckResult fun id(): Id

  @CheckResult fun isReal(): Boolean

  @CheckResult fun makeReal(): FridgeEntry

  @CheckResult fun archivedAt(): Date?

  @CheckResult fun isArchived(): Boolean

  @CheckResult fun invalidateArchived(): FridgeEntry

  @CheckResult fun archive(): FridgeEntry

  data class Id(val id: String) {

    @CheckResult
    fun isEmpty(): Boolean {
      return id.isBlank()
    }

    companion object {

      @JvmField val EMPTY = Id("")
    }
  }

  companion object {

    const val DEFAULT_NAME = "My Fridge"

    @CheckResult
    fun create(name: String): FridgeEntry {
      return JsonMappableFridgeEntry(
          Id(IdGenerator.generate()), name, currentDate(), null, isReal = true)
    }

    @CheckResult
    @JvmOverloads
    fun create(
        entry: FridgeEntry,
        name: String = entry.name(),
        createdTime: Date = entry.createdTime(),
        archivedAt: Date? = entry.archivedAt(),
        isReal: Boolean,
    ): FridgeEntry {
      return JsonMappableFridgeEntry(entry.id(), name, createdTime, archivedAt, isReal)
    }
  }
}
