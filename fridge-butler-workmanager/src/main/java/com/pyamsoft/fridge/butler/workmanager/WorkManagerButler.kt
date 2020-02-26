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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.workmanager.worker.BaseWorker
import com.pyamsoft.fridge.butler.workmanager.worker.ExpirationWorker
import com.pyamsoft.fridge.butler.workmanager.worker.LocationWorker
import com.pyamsoft.fridge.butler.workmanager.worker.NeededWorker
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
        type: WorkType,
        params: Butler.Parameters
    ) {
        val request = OneTimeWorkRequest.Builder(worker)
            .addTag(tag)
            .setConstraints(generateConstraints())
            .apply {
                // We must manually reschedule since PeriodicWork jobs do not repeat on Samsung...
                if (type is WorkType.Periodic) {
                    setInitialDelay(type.time, TimeUnit.MILLISECONDS)
                }
                setInputData(params.toInputData())
            }
            .build()

        workManager().enqueue(request)
        Timber.d("Queue work [$tag]: ${request.id}")
    }

    private fun scheduleExpirationWork(params: Butler.Parameters, type: WorkType) {
        cancelExpirationReminder()
        schedule(ExpirationWorker::class.java, EXPIRATION_TAG, type, params)
    }

    override fun remindExpiration(params: Butler.Parameters) {
        scheduleExpirationWork(params, WorkType.Instant)
    }

    override suspend fun scheduleRemindExpiration(params: Butler.Parameters) {
        val time = preferences.getNotificationPeriod()
        scheduleExpirationWork(params, WorkType.Periodic(time))
    }

    override fun cancelExpirationReminder() {
        workManager().cancelAllWorkByTag(EXPIRATION_TAG)
    }

    private fun scheduleNeeded(params: Butler.Parameters, type: WorkType) {
        cancelLocationReminder()
        schedule(NeededWorker::class.java, LOCATION_TAG, type, params)
    }

    override fun remindNeeded(params: Butler.Parameters) {
        scheduleNeeded(params, WorkType.Instant)
    }

    override suspend fun scheduleRemindNeeded(params: Butler.Parameters) {
        val time = preferences.getNotificationPeriod()
        scheduleNeeded(params, WorkType.Periodic(time))
    }

    override fun cancelNeededReminder() {
        workManager().cancelAllWorkByTag(NEEDED_TAG)
    }

    private fun scheduleLocation(params: Butler.Parameters, type: WorkType) {
        cancelLocationReminder()
        schedule(LocationWorker::class.java, LOCATION_TAG, type, params)
    }

    override fun remindLocation(params: Butler.Parameters) {
        scheduleLocation(params, WorkType.Instant)
    }

    override suspend fun scheduleRemindLocation(params: Butler.Parameters) {
        val time = preferences.getNotificationPeriod()
        scheduleLocation(params, WorkType.Periodic(time))
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
        private const val NEEDED_TAG = "Needed Reminder 1"
        private const val LOCATION_TAG = "Location Reminder 1"
    }

    @CheckResult
    private fun Butler.Parameters.toInputData(): Data {
        return Data.Builder().putBoolean(
            BaseWorker.FORCE_NOTIFICATION,
            this.forceNotification
        ).build()
    }
}
