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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope

internal abstract class FridgeWorker protected constructor(
    context: Context,
    params: WorkerParameters
) : BaseWorker(context, params) {

    private var fridgeEntryQueryDao: FridgeEntryQueryDao? = null
    private var fridgeItemQueryDao: FridgeItemQueryDao? = null

    final override fun onInject() {
        fridgeEntryQueryDao = Injector.obtain(applicationContext)
        fridgeItemQueryDao = Injector.obtain(applicationContext)
        afterInject()
    }

    protected open fun afterInject() {
    }

    final override fun onTeardown() {
        fridgeEntryQueryDao = null
        fridgeItemQueryDao = null
        afterTeardown()
    }

    protected open fun afterTeardown() {
    }

    protected suspend fun withFridgeData(func: suspend CoroutineScope.(entry: FridgeEntry, items: List<FridgeItem>) -> Unit) {
        coroutineScope {
            requireNotNull(fridgeEntryQueryDao).query(false)
                .forEach { entry ->
                    val items = requireNotNull(fridgeItemQueryDao).query(false, entry.id())
                    func(entry, items)
                }
        }
    }
}
