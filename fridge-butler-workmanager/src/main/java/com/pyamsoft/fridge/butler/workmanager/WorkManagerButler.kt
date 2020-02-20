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
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.ExpirationWorker
import com.pyamsoft.fridge.butler.workmanager.worker.LocationWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WorkManagerButler @Inject internal constructor(
    private val context: Context,
    private val preferences: NotificationPreferences
) : Butler {

    @CheckResult
    private fun workManager(): WorkManager {
        return WorkManager.getInstance(context)
    }

    @CheckResult
    private fun generateConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun <T : ListenableWorker> schedule(
        worker: Class<T>,
        tag: String,
        type: WorkType
    ) {
        val request = OneTimeWorkRequest.Builder(worker)
            .addTag(tag)
            .setConstraints(generateConstraints())
            .apply {
                // We must manually reschedule since PeriodicWork jobs do not repeat on Samsung...
                if (type is WorkType.Periodic) {
                    setInitialDelay(type.time, TimeUnit.MILLISECONDS)
                }
            }
            .build()

        workManager().enqueue(request)
        Timber.d("Queue work [$tag]: ${request.id}")
    }

    private fun scheduleExpirationWork(type: WorkType) {
        cancelExpirationReminder()
        schedule(ExpirationWorker::class.java, EXPIRATION_TAG, type)
    }

    override fun remindExpiration() {
        scheduleExpirationWork(WorkType.Instant)
    }

    override suspend fun scheduleRemindExpiration() {
        val time = preferences.getNotificationPeriod()
        scheduleExpirationWork(WorkType.Periodic(time))
    }

    override fun cancelExpirationReminder() {
        workManager().cancelAllWorkByTag(EXPIRATION_TAG)
    }

    private fun scheduleLocation(type: WorkType) {
        cancelLocationReminder()
        schedule(LocationWorker::class.java, LOCATION_TAG, type)
    }

    override fun remindLocation() {
        scheduleLocation(WorkType.Instant)
    }

    override suspend fun scheduleRemindLocation() {
        val time = preferences.getNotificationPeriod()
        scheduleLocation(WorkType.Periodic(time))
    }

    override fun cancelLocationReminder() {
        workManager().cancelAllWorkByTag(LOCATION_TAG)
    }

    override fun cancel() {
        workManager().cancelAllWork()
    }

    private sealed class WorkType {
        object Instant : WorkType()
        data class Periodic(val time: Long) : WorkType()
    }

    companion object {

        private const val EXPIRATION_TAG = "Expiration Reminder 1"
        private const val LOCATION_TAG = "Location Reminder 1"
    }
}
