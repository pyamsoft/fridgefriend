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

import androidx.annotation.CheckResult
import androidx.work.Constraints
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.entry.FridgeEntry
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Named

internal class WorkManagerButler @Inject internal constructor(
  @Named("entry_worker_class") private val entryWorkerClass: Class<out FridgeEntryWorker>
) : Butler {

  @CheckResult
  private fun workManager(): WorkManager {
    return WorkManager.getInstance()
  }

  @CheckResult
  private fun generateConstraints(): Constraints {
    return Constraints.Builder()
      .setRequiresBatteryNotLow(true)
      .setRequiresStorageNotLow(true)
      .setRequiresCharging(false)
      .build()
  }

  @CheckResult
  private fun generateWorkRequest(entry: FridgeEntry, atDate: Date): WorkRequest {
    val todayInMillis = Calendar.getInstance().time.time
    val dateInMillis = atDate.time
    val timeUntilDateInMillis = dateInMillis - todayInMillis
    require(dateInMillis > todayInMillis)
    require(timeUntilDateInMillis > 0)

    return OneTimeWorkRequest.Builder(entryWorkerClass)
      .addTag(entry.id())
      .setConstraints(generateConstraints())
      .setInitialDelay(timeUntilDateInMillis, MILLISECONDS)
      .build()
  }

  override fun notifyFor(entry: FridgeEntry, atDate: Date) {
    workManager().enqueue(generateWorkRequest(entry, atDate))
  }

  override fun cancel(entry: FridgeEntry) {
    workManager().cancelAllWorkByTag(entry.id())
  }

}
