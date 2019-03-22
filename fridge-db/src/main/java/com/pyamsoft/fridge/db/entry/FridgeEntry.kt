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
  fun copy(name: String): FridgeEntry

  @CheckResult
  fun copy(createdTime: Date): FridgeEntry

  companion object {

    const val DEFAULT_NAME = ""
    val DEFAULT_CREATED_TIME = Date(0)

    @CheckResult
    fun create(name: String = DEFAULT_NAME, createdTime: Date = DEFAULT_CREATED_TIME): FridgeEntry {
      return object : FridgeEntryImpl() {

        private val id = IdGenerator.generate()

        override fun id(): String {
          return id
        }

        override fun name(): String {
          return name
        }

        override fun createdTime(): Date {
          return createdTime
        }
      }
    }

    @CheckResult
    fun create(
      entry: FridgeEntry,
      name: String = entry.name(),
      createdTime: Date = entry.createdTime()
    ): FridgeEntry {
      return object : FridgeEntryImpl() {

        override fun id(): String {
          return entry.id()
        }

        override fun name(): String {
          return name
        }

        override fun createdTime(): Date {
          return createdTime
        }

      }

    }

    private abstract class FridgeEntryImpl protected constructor() : FridgeEntry {

      final override fun copy(name: String): FridgeEntry {
        return FridgeEntry.create(this, name = name)
      }

      final override fun copy(createdTime: Date): FridgeEntry {
        return FridgeEntry.create(
          this,
          createdTime = createdTime
        )
      }

    }

  }

}