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
import androidx.work.ListenableWorker
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.pyamsoft.fridge.butler.Butler
import timber.log.Timber
import java.util.concurrent.TimeUnit.HOURS
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WorkManagerButler @Inject internal constructor() : Butler {

  @CheckResult
  private fun workManager(): WorkManager {
    return WorkManager.getInstance()
  }

  @CheckResult
  private fun generateConstraints(): Constraints {
    return Constraints.Builder()
        .setRequiresDeviceIdle(false)
        .setRequiresBatteryNotLow(true)
        .setRequiresCharging(false)
        .build()
  }

  private fun <T : ListenableWorker> schedule(
    worker: Class<T>,
    time: Long,
    tag: String
  ) {
    val request = PeriodicWorkRequest.Builder(worker, time, HOURS)
        .addTag(tag)
        .setConstraints(generateConstraints())
        .build()

    Timber.d("Queue repeating work: $tag")
    workManager().enqueue(request)
  }

  override fun schedule() {
    // Schedule the same work twice but one requires idle and one does not
    schedule(ExpirationWorker::class.java, 1, EXPIRATION_TAG)
  }

  override fun cancel() {
    Timber.d("Cancel all pending work")
    workManager().cancelAllWork()
  }

  companion object {

    private const val EXPIRATION_TAG = "WorkManagerButler: Expiration Reminder"
  }

}
