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
import com.pyamsoft.fridge.db.ConsumableModel
import com.pyamsoft.fridge.db.IdGenerator
import java.util.Date

interface FridgeItem : ConsumableModel<FridgeItem> {

  @CheckResult
  fun id(): String

  @CheckResult
  fun entryId(): String

  @CheckResult
  fun count(): Int

  @CheckResult
  fun purchaseTime(): Date?

  @CheckResult
  fun expireTime(): Date?

  @CheckResult
  fun presence(): Presence

  @CheckResult
  fun isReal(): Boolean

  @CheckResult
  fun count(count: Int): FridgeItem

  @CheckResult
  fun expireTime(expireTime: Date): FridgeItem

  @CheckResult
  fun invalidateExpiration(): FridgeItem

  @CheckResult
  fun purchaseTime(purchaseTime: Date): FridgeItem

  @CheckResult
  fun invalidatePurchase(): FridgeItem

  @CheckResult
  fun presence(presence: Presence): FridgeItem

  @CheckResult
  fun makeReal(): FridgeItem

  enum class Presence {
    HAVE,
    NEED
  }

  companion object {

    private const val EMPTY_NAME = ""
    private const val DEFAULT_COUNT = 1
    private val DEFAULT_PRESENCE = Presence.NEED

    @CheckResult
    fun isValidName(name: String): Boolean {
      return name.isNotBlank() && name != EMPTY_NAME
    }

    @CheckResult
    @JvmOverloads
    fun create(
      id: String = IdGenerator.generate(),
      entryId: String
    ): FridgeItem {
      return create(
          id, entryId, EMPTY_NAME, DEFAULT_COUNT, Date(), null, null, DEFAULT_PRESENCE, false
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
      purchaseTime: Date?,
      expireTime: Date?,
      presence: Presence,
      isReal: Boolean
    ): FridgeItem {
      return JsonMappableFridgeItem(
          id,
          entryId,
          name,
          count,
          createdTime,
          purchaseTime,
          expireTime,
          presence,
          consumptionDate = null,
          spoiledDate = null,
          isReal = isReal
      )
    }

    @CheckResult
    @JvmOverloads
    fun create(
      item: FridgeItem,
      name: String = item.name(),
      count: Int = item.count(),
      createdTime: Date = item.createdTime(),
      expireTime: Date? = item.expireTime(),
      purchaseTime: Date? = item.purchaseTime(),
      presence: Presence = item.presence(),
      consumptionDate: Date? = item.consumptionDate(),
      spoiledDate: Date? = item.spoiledDate(),
      isReal: Boolean
    ): FridgeItem {
      return JsonMappableFridgeItem(
          item.id(),
          item.entryId(),
          name,
          count,
          createdTime,
          purchaseTime,
          expireTime,
          presence,
          consumptionDate,
          spoiledDate,
          isReal
      )
    }

  }

}

@CheckResult
fun FridgeItem.isArchived(): Boolean {
  return this.isConsumed() || this.isSpoiled()
}
