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

package com.pyamsoft.fridge.work

import android.content.Context
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.butler.workmanager.FridgeEntryWorker
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.ui.Injector
import javax.inject.Inject

internal class FridgeEntryWorkerImpl internal constructor(
  context: Context,
  params: WorkerParameters
) : FridgeEntryWorker(context, params) {

  @JvmField @Inject internal var entryQueryDao: FridgeEntryQueryDao? = null
  @JvmField @Inject internal var itemQueryDao: FridgeItemQueryDao? = null

  override fun entryQueryDao(): FridgeEntryQueryDao {
    return requireNotNull(entryQueryDao)
  }

  override fun itemQueryDao(): FridgeItemQueryDao {
    return requireNotNull(itemQueryDao)
  }

  override fun inject() {
    Injector.obtain<FridgeComponent>(applicationContext)
      .inject(this)
  }
}
