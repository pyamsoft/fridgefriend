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
 */

package com.pyamsoft.fridge.butler.runner

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.notification.NotificationPreferences
import com.pyamsoft.fridge.butler.params.BaseParameters
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Calendar
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal abstract class BaseRunner<P : BaseParameters> protected constructor(
    private val handler: NotificationHandler,
    private val butler: Butler,
    private val notificationPreferences: NotificationPreferences,
    private val butlerPreferences: ButlerPreferences
) {

    private suspend fun teardown(params: P) {
        reschedule(butler, params)
    }

    protected suspend inline fun notification(crossinline func: suspend (handler: NotificationHandler) -> Unit) {
        func(handler)
    }

    protected open suspend fun reschedule(butler: Butler, params: P) {
    }

    @CheckResult
    suspend fun doWork(
        id: UUID,
        tags: Set<String>,
        params: P
    ): WorkResult = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        val identifier = identifier(id, tags)
        try {
            performWork(butlerPreferences, params)
            success(identifier)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                cancelled(identifier, e)
            } else {
                fail(identifier, e)
            }
        } finally {
            teardown(params)
        }
    }

    protected abstract suspend fun performWork(
        preferences: ButlerPreferences,
        params: P
    )

    @CheckResult
    private fun success(identifier: String): WorkResult {
        Timber.d("Worker completed successfully $identifier")
        return WorkResult.Success
    }

    @CheckResult
    private fun fail(identifier: String, throwable: Throwable): WorkResult {
        Timber.e(throwable, "Worker failed to complete $identifier")
        return WorkResult.Failure
    }

    @CheckResult
    private fun cancelled(identifier: String, throwable: CancellationException): WorkResult {
        Timber.w(throwable, "Worker was cancelled $identifier")
        return WorkResult.Failure
    }

    @CheckResult
    private fun identifier(id: UUID, tags: Set<String>): String {
        return "[ id=$id, tags=$tags ]"
    }

    @CheckResult
    protected suspend fun Calendar.isAllowedToNotify(force: Boolean, lastNotified: Long): Boolean {
        if (notificationPreferences.isDoNotDisturb(this)) {
            Timber.w("Do not send notification before 7AM and after 10PM")
            return false
        }

        return if (force || notificationPreferences.canNotify(this, lastNotified)) {
            if (force) {
                Timber.d("Force notification post")
            }
            true
        } else {
            Timber.w("Do not send notification since last one was sent so recently")
            false
        }
    }
}
