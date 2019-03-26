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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.IdGenerator
import com.pyamsoft.fridge.db.entry.FridgeEntry
import java.util.Date

interface FridgeItem {

  @CheckResult
  fun id(): String

  @CheckResult
  fun entryId(): String

  @CheckResult
  fun name(): String

  @CheckResult
  fun expireTime(): Date

  @CheckResult
  fun presence(): Presence

  @CheckResult
  fun copy(name: String): FridgeItem

  @CheckResult
  fun copy(expireTime: Date): FridgeItem

  @CheckResult
  fun copy(presence: Presence): FridgeItem

  enum class Presence {
    HAVE,
    NEED
  }

  companion object {

    const val DEFAULT_NAME = ""
    val DEFAULT_PRESENCE = Presence.NEED
    val DEFAULT_EXPIRE_TIME = Date(0)

    @CheckResult
    fun empty(): FridgeItem {
      return object : FridgeItemImpl() {

        override fun id(): String {
          return ""
        }

        override fun entryId(): String {
          return ""
        }

        override fun name(): String {
          return DEFAULT_NAME
        }

        override fun expireTime(): Date {
          return DEFAULT_EXPIRE_TIME
        }

        override fun presence(): Presence {
          return DEFAULT_PRESENCE
        }

      }
    }

    @CheckResult
    fun create(entry: FridgeEntry): FridgeItem {
      return create(entry, DEFAULT_NAME, DEFAULT_EXPIRE_TIME, DEFAULT_PRESENCE)
    }

    @CheckResult
    fun create(
      entry: FridgeEntry,
      name: String,
      expireTime: Date,
      presence: Presence
    ): FridgeItem {
      return object : FridgeItemImpl() {

        private val id = IdGenerator.generate()

        override fun id(): String {
          return id
        }

        override fun entryId(): String {
          return entry.id()
        }

        override fun name(): String {
          return name
        }

        override fun expireTime(): Date {
          return expireTime
        }

        override fun presence(): Presence {
          return presence
        }

      }
    }

    @CheckResult
    fun create(
      item: FridgeItem,
      name: String = item.name(),
      expireTime: Date = item.expireTime(),
      presence: Presence = item.presence()
    ): FridgeItem {
      return object : FridgeItemImpl() {

        override fun id(): String {
          return item.id()
        }

        override fun entryId(): String {
          return item.entryId()
        }

        override fun name(): String {
          return name
        }

        override fun expireTime(): Date {
          return expireTime
        }

        override fun presence(): Presence {
          return presence
        }

      }
    }

    protected abstract class FridgeItemImpl protected constructor() : FridgeItem {

      final override fun copy(name: String): FridgeItem {
        return create(this, name = name)
      }

      final override fun copy(expireTime: Date): FridgeItem {
        return create(this, expireTime = expireTime)
      }

      final override fun copy(presence: Presence): FridgeItem {
        return create(this, presence = presence)
      }
    }
  }
}