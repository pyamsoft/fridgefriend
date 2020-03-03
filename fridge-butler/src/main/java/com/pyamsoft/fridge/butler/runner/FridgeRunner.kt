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

package com.pyamsoft.fridge.butler.runner

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.params.BaseParameters
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

internal abstract class FridgeRunner<P : BaseParameters> protected constructor(
    handler: NotificationHandler,
    butler: Butler,
    notificationPreferences: NotificationPreferences,
    butlerPreferences: ButlerPreferences,
    enforcer: Enforcer,
    private val fridgeEntryQueryDao: FridgeEntryQueryDao,
    private val fridgeItemQueryDao: FridgeItemQueryDao
) : BaseRunner<P>(
    handler,
    butler,
    notificationPreferences,
    butlerPreferences,
    enforcer
) {

    protected suspend fun withFridgeData(func: suspend CoroutineScope.(entry: FridgeEntry, items: List<FridgeItem>) -> Unit) {
        coroutineScope {
            fridgeEntryQueryDao.query(false)
                .forEach { entry ->
                    val items = fridgeItemQueryDao.query(false, entry.id())
                    func(entry, items)
                }
        }
    }
}
