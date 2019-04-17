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
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableFridgeItem internal constructor(
  internal val id: String,
  internal val entryId: String,
  internal val name: String,
  internal val expireTime: Date,
  internal val presence: Presence,
  internal val isReal: Boolean,
  internal val isArchived: Boolean
) : FridgeItem {

  override fun id(): String {
    return id
  }

  override fun entryId(): String {
    return entryId
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

  override fun isReal(): Boolean {
    return isReal
  }

  override fun isArchived(): Boolean {
    return isArchived
  }

  override fun name(name: String): FridgeItem {
    return this.copy(name = name)
  }

  override fun expireTime(expireTime: Date): FridgeItem {
    return this.copy(expireTime = expireTime)
  }

  override fun presence(presence: Presence): FridgeItem {
    return this.copy(presence = presence)
  }

  override fun makeReal(): FridgeItem {
    return this.copy(isReal = true)
  }

  override fun archive(): FridgeItem {
    return this.copy(isArchived = true)
  }

  companion object {

    @JvmStatic
    @CheckResult
    fun from(item: FridgeItem): JsonMappableFridgeItem {
      return JsonMappableFridgeItem(
        item.id(),
        item.entryId(),
        item.name(),
        item.expireTime(),
        item.presence(),
        item.isReal(),
        item.isArchived()
      )
    }
  }
}