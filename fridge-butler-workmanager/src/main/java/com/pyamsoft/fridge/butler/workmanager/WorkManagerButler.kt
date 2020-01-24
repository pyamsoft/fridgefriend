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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.workmanager.expiration.ExpirationWorker
import com.pyamsoft.fridge.butler.workmanager.geofence.GeofenceNotifierWorker
import com.pyamsoft.fridge.butler.workmanager.geofence.GeofenceRegistrationWorker
import com.pyamsoft.fridge.butler.workmanager.locator.LocationWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class WorkManagerButler @Inject internal constructor(
    private val context: Context
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
        data: Map<String, Any?>?
    ) {
        val builder = when (type) {
            is WorkType.Instant -> OneTimeWorkRequest.Builder(worker)
            is WorkType.Periodic -> PeriodicWorkRequest.Builder(worker, type.time, type.unit)
        }

        val request = builder
            .addTag(tag)
            .setConstraints(generateConstraints())
            .apply {
                if (data != null) {
                    setInputData(Data.Builder().putAll(data).build())
                }
            }
            .build()

        workManager().enqueue(request)
        Timber.d("Queue work [$tag]: ${request.id}")
    }

    private fun scheduleExpirationWork(type: WorkType) {
        cancelExpirationReminder()
        schedule(ExpirationWorker::class.java, EXPIRATION_TAG, type, null)
    }

    override fun remindExpiration() {
        scheduleExpirationWork(WorkType.Periodic(RESCHEDULE_TIME, RESCHEDULE_UNIT))
    }

    override fun cancelExpirationReminder() {
        workManager().cancelAllWorkByTag(EXPIRATION_TAG)
    }

    private fun scheduleGeofenceWork(type: WorkType) {
        unregisterGeofences()
        schedule(GeofenceRegistrationWorker::class.java, GEOFENCE_REGISTRATION_TAG, type, null)
    }

    override fun registerGeofences() {
        scheduleGeofenceWork(WorkType.Periodic(RESCHEDULE_TIME, RESCHEDULE_UNIT))
    }

    override fun unregisterGeofences() {
        workManager().cancelAllWorkByTag(GEOFENCE_REGISTRATION_TAG)
    }

    override fun processGeofences(
        currentLatitude: Double,
        currentLongitude: Double,
        fences: List<String>
    ) {
        cancelGeofenceProcessing()
        schedule(
            GeofenceNotifierWorker::class.java, GEOFENCE_NOTIFY_TAG, WorkType.Instant,
            mapOf(
                GeofenceNotifierWorker.KEY_FENCES to fences.toTypedArray(),
                GeofenceNotifierWorker.KEY_CURRENT_LATITUDE to currentLatitude,
                GeofenceNotifierWorker.KEY_CURRENT_LONGITUDE to currentLongitude
            )
        )
    }

    override fun cancelGeofenceProcessing() {
        workManager().cancelAllWorkByTag(GEOFENCE_NOTIFY_TAG)
    }

    private fun scheduleLocation(type: WorkType) {
        cancelLocationReminder()
        schedule(LocationWorker::class.java, LOCATION_TAG, type, null)
    }

    override fun remindLocation() {
        scheduleLocation(WorkType.Periodic(RESCHEDULE_TIME, RESCHEDULE_UNIT))
    }

    override fun cancelLocationReminder() {
        workManager().cancelAllWorkByTag(LOCATION_TAG)
    }

    override fun cancel() {
        workManager().cancelAllWork()
    }

    private sealed class WorkType {
        object Instant : WorkType()
        data class Periodic(val time: Long, val unit: TimeUnit) : WorkType()
    }

    companion object {

        private const val EXPIRATION_TAG = "Expiration Reminder 1"
        private const val GEOFENCE_REGISTRATION_TAG = "Geofence Registration Reminder 1"
        private const val GEOFENCE_NOTIFY_TAG = "Geofence Notifier Reminder 1"
        private const val LOCATION_TAG = "Location Reminder 1"

        private const val RESCHEDULE_TIME = 2L
        private val RESCHEDULE_UNIT = TimeUnit.HOURS
    }
}
