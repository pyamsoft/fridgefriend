/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.butler.notification

import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.notify.NotifyData

data class NeededItemNotifyData internal constructor(
    val entry: FridgeEntry,
    val items: List<FridgeItem>
) : NotifyData

data class NearbyItemNotifyData internal constructor(
    val name: String,
    val items: List<FridgeItem>
) : NotifyData

data class ExpiringItemNotifyData internal constructor(
    val entry: FridgeEntry,
    val items: List<FridgeItem>
) : NotifyData

data class ExpiredItemNotifyData internal constructor(
    val entry: FridgeEntry,
    val items: List<FridgeItem>
) : NotifyData

object NightlyNotifyData : NotifyData