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

package com.pyamsoft.fridge.butler.workmanager.geofence

import android.content.Context
import com.pyamsoft.fridge.butler.ButlerNotifications
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone

internal object GeofenceNotifications {

  private const val NEEDED_CHANNEL_ID = "fridge_needed_reminders_channel_v1"

  private fun notifyNeeded(
    context: Context,
    entry: FridgeEntry,
    storeName: String,
    items: List<FridgeItem>
  ) {
    ButlerNotifications.notify(
        entry,
        context,
        NEEDED_CHANNEL_ID,
        "Needed Reminders",
        "Reminders for items that you still need to purchase."
    ) { builder ->
      val extra = ButlerNotifications.getExtraItems(items)
      return@notify builder
          .setContentTitle("Nearby Reminder for $storeName")
          .setContentText("You still need '${items.first().name()}' $extra")
          .build()
    }
  }

  @JvmStatic
  fun notifyNeeded(
    context: Context,
    entry: FridgeEntry,
    store: NearbyStore,
    items: List<FridgeItem>
  ) {
    notifyNeeded(context, entry, store.name(), items)
  }

  @JvmStatic
  fun notifyNeeded(
    context: Context,
    entry: FridgeEntry,
    zone: NearbyZone,
    items: List<FridgeItem>
  ) {
    notifyNeeded(context, entry, zone.name(), items)
  }

}
