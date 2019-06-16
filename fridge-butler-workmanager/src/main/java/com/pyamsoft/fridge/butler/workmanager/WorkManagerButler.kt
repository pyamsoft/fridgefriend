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
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.expiration.ExpirationWorker
import com.pyamsoft.fridge.butler.workmanager.locator.LocationWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
    tag: String,
    time: Long,
    unit: TimeUnit
  ) {
    val request = OneTimeWorkRequest.Builder(worker)
        .setInitialDelay(time, unit)
        .addTag(tag)
        .setConstraints(generateConstraints())
        .build()

    workManager().enqueue(request)
    Timber.d("Queue work [$tag]: ${request.id}")
  }

  private fun scheduleExpirationWork(
    time: Long,
    unit: TimeUnit
  ) {
    schedule(ExpirationWorker::class.java, EXPIRATION_TAG, time, unit)
  }

  override fun remindExpiration(
    time: Long,
    unit: TimeUnit
  ) {
    // Schedule the same work twice but one requires idle and one does not
    scheduleExpirationWork(time, unit)
  }

  override fun cancelExpirationReminder() {
    Timber.d("Cancel all pending expiration reminders")
    workManager().cancelAllWorkByTag(EXPIRATION_TAG)
  }

  override fun remindLocation(
    time: Long,
    unit: TimeUnit
  ) {
    schedule(LocationWorker::class.java, LOCATION_TAG, time, unit)
  }

  override fun cancelLocationReminder() {
    Timber.d("Cancel all pending location reminders")
    workManager().cancelAllWorkByTag(LOCATION_TAG)
  }

  override fun cancel() {
    Timber.d("Cancel all pending work")
    workManager().cancelAllWork()
  }

  companion object {

    private const val EXPIRATION_TAG = "WorkManagerButler: Expiration Reminder"
    private const val LOCATION_TAG = "WorkManagerButler: Location Reminder"
  }

}
