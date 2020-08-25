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

package com.pyamsoft.fridge.butler.workmanager

import android.content.Context
import androidx.annotation.CheckResult
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.Worker
import com.google.common.util.concurrent.ListenableFuture
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.order.Order
import com.pyamsoft.fridge.butler.order.OrderParameters
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
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
    private val context: Context
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

    private fun schedule(
        work: Class<*>,
        tag: String,
        type: WorkType,
        inputData: Data
    ) {
        Enforcer.assertOffMainThread()

        // Basically, this is shit, but hey its Android!
        // Please make sure your orders use a class that implements a worker, thanks.
        @Suppress("UNCHECKED_CAST")
        val worker = work as Class<out Worker>

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

    override suspend fun placeOrder(order: Order) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        cancelOrder(order)
        schedule(order.work(), order.tag(), WorkType.Instant, order.parameters().toInputData())
    }

    override suspend fun scheduleOrder(order: Order) = withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        cancelOrder(order)
        schedule(
            order.work(),
            order.tag(),
            WorkType.Periodic(order.period()),
            order.parameters().toInputData()
        )
    }

    override suspend fun cancelOrder(order: Order) =
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            workManager().cancelAllWorkByTag(order.tag()).await()
        }

    override suspend fun cancel() {
        withContext(context = Dispatchers.Default) {
            Enforcer.assertOffMainThread()
            workManager().cancelAllWork().await()
        }
    }

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
        this.addListener({
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

@CheckResult
private fun OrderParameters.toInputData(): Data {
    var builder = Data.Builder()
    val booleans = this.getBooleanParameters()
    for (entry in booleans) {
        builder = builder.putBoolean(entry.key, entry.value)
    }
    return builder.build()

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
