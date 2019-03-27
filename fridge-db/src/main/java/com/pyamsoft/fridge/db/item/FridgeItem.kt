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
  fun name(name: String): FridgeItem

  @CheckResult
  fun expireTime(expireTime: Date): FridgeItem

  @CheckResult
  fun presence(presence: Presence): FridgeItem

  enum class Presence {
    HAVE,
    NEED
  }

  companion object {

    const val EMPTY_NAME = ""
    val EMPTY_CREATED_TIME = Date(0)
    val DEFAULT_PRESENCE = Presence.NEED

    @CheckResult
    fun empty(): FridgeItem {
      return create("", "", EMPTY_NAME, EMPTY_CREATED_TIME, DEFAULT_PRESENCE)
    }

    @CheckResult
    @JvmOverloads
    fun create(id: String = IdGenerator.generate(), entryId: String): FridgeItem {
      return create(id, entryId, EMPTY_NAME, Date(), DEFAULT_PRESENCE)
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      entryId: String,
      name: String,
      expireTime: Date,
      presence: Presence
    ): FridgeItem {
      return JsonMappableFridgeItem(id, entryId, name, expireTime, presence)
    }

    @CheckResult
    fun create(
      item: FridgeItem,
      name: String = item.name(),
      expireTime: Date = item.expireTime(),
      presence: Presence = item.presence()
    ): FridgeItem {
      return JsonMappableFridgeItem(item.id(), item.entryId(), name, expireTime, presence)
    }

  }

}