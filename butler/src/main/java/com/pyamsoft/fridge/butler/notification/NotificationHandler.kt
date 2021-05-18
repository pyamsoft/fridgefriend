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

package com.pyamsoft.fridge.butler.notification

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.notify.NotifyId

interface NotificationHandler {

  fun cancel(notificationId: NotifyId)

  @CheckResult fun notifyNeeded(entry: FridgeEntry, items: List<FridgeItem>): Boolean

  @CheckResult fun notifyExpiring(entry: FridgeEntry, items: List<FridgeItem>): Boolean

  @CheckResult fun notifyExpired(entry: FridgeEntry, items: List<FridgeItem>): Boolean

  @CheckResult fun notifyNightly(): Boolean

  companion object {

    const val KEY_NOTIFICATION_ID = "key_notification_id"

    const val KEY_ENTRY_ID = "key_entry_id"
    const val KEY_PRESENCE_TYPE = "key_presence_type"
  }
}
