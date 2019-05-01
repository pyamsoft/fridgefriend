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

package com.pyamsoft.fridge.butler.workmanager

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.InjectableWorker
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import io.reactivex.Single

abstract class FridgeEntryWorker protected constructor(
  context: Context,
  params: WorkerParameters
) : RxWorker(context, params), InjectableWorker {

  @CheckResult
  protected abstract fun entryQueryDao(): FridgeEntryQueryDao

  @CheckResult
  protected abstract fun itemQueryDao(): FridgeItemQueryDao

  final override fun createWork(): Single<Result> {
    inject()

    return Single.error { TODO() }
  }

}
