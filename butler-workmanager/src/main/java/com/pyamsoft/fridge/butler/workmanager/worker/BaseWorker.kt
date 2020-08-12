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

package com.pyamsoft.fridge.butler.workmanager.worker

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.pyamsoft.fridge.butler.injector.BaseInjector
import com.pyamsoft.fridge.butler.params.BaseParameters
import com.pyamsoft.fridge.butler.runner.WorkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal abstract class BaseWorker<P : BaseParameters> protected constructor(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context.applicationContext, params) {

    final override suspend fun doWork(): Result = withContext(context = Dispatchers.Default) {
        val injector = getInjector(applicationContext)
        val result = injector.run(id, tags.toSet(), getParams(inputData))
        if (result == WorkResult.Success) Result.success() else Result.failure()
    }

    @CheckResult
    protected abstract fun getInjector(context: Context): BaseInjector<P>

    @CheckResult
    protected abstract fun getParams(data: Data): P

    companion object {

        internal const val FORCE_NEEDED_NOTIFICATION = "force_needed_notifications_v1"
        internal const val FORCE_EXPIRING_NOTIFICATION = "force_expiring_notifications_v1"
    }
}
