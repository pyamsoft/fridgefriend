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
import androidx.work.Operation
import androidx.work.WorkManager
import com.google.common.util.concurrent.ListenableFuture
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.butler.params.ItemParameters
import com.pyamsoft.fridge.butler.params.LocationParameters
import com.pyamsoft.fridge.butler.workmanager.worker.BaseWorker
import com.pyamsoft.fridge.butler.workmanager.worker.ItemWorker
import com.pyamsoft.fridge.butler.workmanager.worker.LocationWorker
import com.pyamsoft.fridge.butler.workmanager.worker.NightlyWorker
import com.pyamsoft.fridge.core.today
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Calendar
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
internal class WorkManagerButler @Inject internal constructor(
    private val context: Context,
    private val preferences: NotificationPreferences
) : Butler {

    @CheckResult
    private fun workManager(): WorkManager {
        Enforcer.assertOffMainThread()
        return WorkManager.getInstance(context)
    }

    @CheckResult
    private fun generateConstraints(): Constraints {
        Enforcer.assertOffMainThread()
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun <T : ListenableWorker> schedule(
        worker: Class<T>,
        tag: String,
        type: WorkType,
        inputData: Data
    ) {
        Enforcer.assertOffMainThread()
        val request = OneTimeWorkRequest.Builder(worker)
            .addTag(tag)
            .setConstraints(generateConstraints())
            .apply {
                // We must manually reschedule since PeriodicWork jobs do not repeat on Samsung...
                if (type is WorkType.Periodic) {
                    setInitialDelay(type.time, TimeUnit.MILLISECONDS)
                }
                setInputData(inputData)
            }
            .build()

        workManager().enqueue(request)
        Timber.d("Queue work [$tag]: ${request.id}")
    }

    private suspend fun scheduleItemWork(params: ItemParameters, type: WorkType) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            cancelItemsReminder()
            schedule(ItemWorker::class.java, ITEM_TAG, type, params.toInputData())
        }

    override suspend fun remindItems(params: ItemParameters) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            scheduleItemWork(params, WorkType.Instant)
        }

    override suspend fun scheduleRemindItems(params: ItemParameters) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            val time = preferences.getNotificationPeriod()
            scheduleItemWork(params, WorkType.Periodic(time))
        }

    override suspend fun cancelItemsReminder() = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        workManager().cancelAllWorkByTag(ITEM_TAG).await()
    }

    private suspend fun scheduleLocation(params: LocationParameters, type: WorkType) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            cancelLocationReminder()
            schedule(LocationWorker::class.java, LOCATION_TAG, type, params.toInputData())
        }

    override suspend fun remindLocation(params: LocationParameters) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            scheduleLocation(params, WorkType.Instant)
        }

    override suspend fun scheduleRemindLocation(params: LocationParameters) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            val time = preferences.getNotificationPeriod()
            scheduleLocation(params, WorkType.Periodic(time))
        }

    override suspend fun cancelLocationReminder() = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        workManager().cancelAllWorkByTag(LOCATION_TAG).await()
    }

    private suspend fun scheduleNightly(params: EmptyParameters, type: WorkType) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            cancelNightlyReminder()
            schedule(NightlyWorker::class.java, NIGHTLY_TAG, type, params.toInputData())
        }

    override suspend fun scheduleRemindNightly(params: EmptyParameters) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()

            // Get the time now
            val now = today { set(Calendar.MILLISECOND, 0) }

            // Get the time in the evening at our notification hour
            val evening = today {
                set(Calendar.HOUR_OF_DAY, 8 + 12)
                set(Calendar.MILLISECOND, 0)
            }

            // If it is after this evening, schedule for tomorrow evening
            if (now.after(evening)) {
                evening.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Calculate the difference
            val nowMillis = now.timeInMillis
            val eveningMillis = evening.timeInMillis
            val difference = eveningMillis - nowMillis
            scheduleNightly(params, WorkType.Periodic(difference))
        }

    override suspend fun cancelNightlyReminder() = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        workManager().cancelAllWorkByTag(NIGHTLY_TAG).await()
    }

    override suspend fun cancel() = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        cancelItemsReminder()
        cancelLocationReminder()
        cancelNightlyReminder()
    }

    private suspend fun Operation.await() {
        Enforcer.assertOffMainThread()
        this.result.await()
    }

    // Copied out of androidx.work.ListenableFuture
    // since this extension is library private otherwise...
    @Suppress("BlockingMethodInNonBlockingContext")
    private suspend fun <R> ListenableFuture<R>.await(): R {
        Enforcer.assertOffMainThread()

        // Fast path
        if (this.isDone) {
            try {
                return this.get()
            } catch (e: ExecutionException) {
                throw e.cause ?: e
            }
        }

        return suspendCancellableCoroutine { continuation ->
            Enforcer.assertOffMainThread()
            this.addListener(Runnable {
                Enforcer.assertOffMainThread()
                try {
                    continuation.resume(this.get())
                } catch (throwable: Throwable) {
                    val cause = throwable.cause ?: throwable
                    when (throwable) {
                        is CancellationException -> continuation.cancel(cause)
                        else -> continuation.resumeWithException(cause)
                    }
                }
            }, ButlerExecutor)
        }
    }

    private object ButlerExecutor : Executor {

        override fun execute(command: Runnable) {
            command.run()
        }
    }

    private sealed class WorkType {
        object Instant : WorkType()
        data class Periodic(val time: Long) : WorkType()
    }

    companion object {

        private const val ITEM_TAG = "Items Reminder 1"
        private const val LOCATION_TAG = "Location Reminder 1"
        private const val NIGHTLY_TAG = "Nightly Reminder 1"
    }

    @CheckResult
    private fun LocationParameters.toInputData(): Data {
        return Data.Builder()
            .putBoolean(BaseWorker.FORCE_NEEDED_NOTIFICATION, this.forceNotifyNeeded)
            .build()
    }

    @CheckResult
    private fun ItemParameters.toInputData(): Data {
        return Data.Builder()
            .putBoolean(BaseWorker.FORCE_NEEDED_NOTIFICATION, this.forceNotifyNeeded)
            .putBoolean(BaseWorker.FORCE_EXPIRING_NOTIFICATION, this.forceNotifyExpiring)
            .build()
    }

    @CheckResult
    private fun EmptyParameters.toInputData(): Data {
        return Data.EMPTY
    }
}
