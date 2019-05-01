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
import androidx.work.RxWorker
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.pydroid.ui.Injector
import io.reactivex.Single

internal class FridgeEntryWorker internal constructor(
  context: Context,
  params: WorkerParameters
) : RxWorker(context, params) {

  private var fridgeEntryQueryDao: FridgeEntryQueryDao? = null
  private var fridgeItemQueryDao: FridgeItemQueryDao? = null

  private fun inject() {
    fridgeEntryQueryDao = Injector.obtain(applicationContext)
    fridgeItemQueryDao = Injector.obtain(applicationContext)
  }

  private fun teardown() {
    fridgeEntryQueryDao = null
    fridgeItemQueryDao = null
  }

  override fun onStopped() {
    super.onStopped()
    teardown()
  }

  override fun createWork(): Single<Result> {
    inject()
    return Single.defer {
      return@defer Single.just(Result.success())
    }.doAfterTerminate { teardown() }
  }

}
