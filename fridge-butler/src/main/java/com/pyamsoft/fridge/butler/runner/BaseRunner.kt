/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.NotificationPreferences
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Calendar
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal abstract class BaseRunner protected constructor(
    private val handler: NotificationHandler,
    private val butler: Butler,
    private val notificationPreferences: NotificationPreferences,
    private val butlerPreferences: ButlerPreferences,
    private val fridgeItemPreferences: FridgeItemPreferences,
    private val enforcer: Enforcer
) {

    private suspend fun teardown() {
        reschedule(butler)
    }

    protected suspend fun notification(func: suspend (handler: NotificationHandler) -> Unit) {
        func(handler)
    }

    protected open suspend fun reschedule(butler: Butler) {
    }

    @CheckResult
    suspend fun doWork(): WorkResult {
        enforcer.assertNotOnMainThread()

        return try {
            performWork(butlerPreferences, fridgeItemPreferences)
            success()
        } catch (e: Throwable) {
            if (e is CancellationException) {
                cancelled(e)
            } else {
                fail(e)
            }
        } finally {
            teardown()
        }
    }

    protected abstract suspend fun performWork(
        preferences: ButlerPreferences,
        fridgeItemPreferences: FridgeItemPreferences
    )

    @CheckResult
    private fun success(): WorkResult {
        Timber.d("Worker completed successfully")
        return WorkResult.Success
    }

    @CheckResult
    private fun fail(throwable: Throwable): WorkResult {
        Timber.e(throwable, "Worker failed to complete")
        return WorkResult.Failure
    }

    @CheckResult
    private fun cancelled(throwable: CancellationException): WorkResult {
        Timber.w(throwable, "Worker was cancelled")
        return WorkResult.Failure
    }

    @CheckResult
    protected suspend fun Calendar.isAllowedToNotify(lastNotified: Long): Boolean {
        val nowInMillis = this.timeInMillis
        return lastNotified + notificationPreferences.getNotificationPeriod() < nowInMillis
    }
}
