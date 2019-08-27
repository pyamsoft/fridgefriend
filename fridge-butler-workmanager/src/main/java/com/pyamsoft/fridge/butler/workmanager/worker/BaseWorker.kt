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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ForegroundState
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.ui.Injector
import kotlinx.coroutines.CancellationException
import timber.log.Timber

internal abstract class BaseWorker protected constructor(
  context: Context,
  params: WorkerParameters
) : CoroutineWorker(context, params) {

  private var handler: NotificationHandler? = null
  private var foregroundState: ForegroundState? = null
  private var butler: Butler? = null
  private var enforcer: Enforcer? = null

  private fun inject() {
    handler = Injector.obtain(applicationContext)
    foregroundState = Injector.obtain(applicationContext)
    butler = Injector.obtain(applicationContext)
    enforcer = Injector.obtain(applicationContext)
    onInject()
  }

  protected abstract fun onInject()

  private fun teardown() {
    handler = null
    foregroundState = null
    butler = null
    enforcer = null
    onTeardown()
  }

  protected inline fun notification(func: (handler: NotificationHandler, foregroundState: ForegroundState) -> Unit) {
    func(requireNotNull(handler), requireNotNull(foregroundState))
  }

  protected abstract fun onTeardown()

  protected abstract fun reschedule(butler: Butler)

  final override suspend fun doWork(): Result {
    inject()
    requireNotNull(enforcer).assertNotOnMainThread()

    try {
      performWork()
      return success()
    } catch (e: Throwable) {
      if (e is CancellationException) {
        return cancelled(e)
      } else {
        return fail(e)
      }
    } finally {
      teardown()
    }
  }

  protected abstract suspend fun performWork()

  @CheckResult
  private fun success(): Result {
    Timber.d("Worker completed successfully")
    reschedule(requireNotNull(butler))
    return Result.success()
  }

  @CheckResult
  private fun fail(throwable: Throwable): Result {
    Timber.e(throwable, "Worker failed to complete")
    reschedule(requireNotNull(butler))
    return Result.failure()
  }

  @CheckResult
  private fun cancelled(throwable: CancellationException): Result {
    Timber.w(throwable, "Worker was cancelled")
    return Result.failure()
  }
}
