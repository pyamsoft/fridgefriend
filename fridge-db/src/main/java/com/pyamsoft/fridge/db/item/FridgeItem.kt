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
import com.pyamsoft.fridge.db.BaseModel
import com.pyamsoft.fridge.db.IdGenerator
import java.util.Date

interface FridgeItem : BaseModel<FridgeItem> {

  @CheckResult
  fun entryId(): String

  @CheckResult
  fun count(): Int

  @CheckResult
  fun expireTime(): Date

  @CheckResult
  fun presence(): Presence

  @CheckResult
  fun isReal(): Boolean

  @CheckResult
  fun count(count: Int): FridgeItem

  @CheckResult
  fun expireTime(expireTime: Date): FridgeItem

  @CheckResult
  fun presence(presence: Presence): FridgeItem

  @CheckResult
  fun makeReal(): FridgeItem

  enum class Presence {
    HAVE,
    NEED
  }

  companion object {

    const val EMPTY_NAME = ""
    val EMPTY_EXPIRE_TIME = Date(0)
    private const val DEFAULT_COUNT = 1
    private val DEFAULT_PRESENCE = Presence.NEED

    @CheckResult
    fun empty(entryId: String): FridgeItem {
      return create("", entryId)
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      entryId: String
    ): FridgeItem {
      return create(
          id, entryId, EMPTY_NAME, DEFAULT_COUNT, Date(), EMPTY_EXPIRE_TIME, DEFAULT_PRESENCE, false
      )
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      entryId: String,
      name: String,
      count: Int,
      createdTime: Date,
      expireTime: Date,
      presence: Presence,
      isReal: Boolean
    ): FridgeItem {
      return JsonMappableFridgeItem(
          id,
          entryId,
          name,
          count,
          createdTime,
          expireTime,
          presence,
          isReal,
          isArchived = false
      )
    }

    @CheckResult
    fun create(
      item: FridgeItem,
      name: String = item.name(),
      count: Int = item.count(),
      createdTime: Date = item.createdTime(),
      expireTime: Date = item.expireTime(),
      presence: Presence = item.presence(),
      isReal: Boolean,
      isArchived: Boolean
    ): FridgeItem {
      return JsonMappableFridgeItem(
          item.id(),
          item.entryId(),
          name,
          count,
          createdTime,
          expireTime,
          presence,
          isReal,
          isArchived
      )
    }

  }

}
