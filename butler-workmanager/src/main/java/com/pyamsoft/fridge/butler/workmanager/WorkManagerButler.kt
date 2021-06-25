/*
 * Copyright 2021 Peter Kenji Yamanaka
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
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.Worker
import com.google.common.util.concurrent.ListenableFuture
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.work.Order
import com.pyamsoft.fridge.butler.work.OrderParameters
import com.pyamsoft.fridge.butler.work.order.ItemOrder
import com.pyamsoft.fridge.butler.work.order.NightlyOrder
import com.pyamsoft.fridge.butler.work.order.PeriodicOrder
import com.pyamsoft.fridge.butler.workmanager.worker.ItemWorker
import com.pyamsoft.fridge.butler.workmanager.worker.NightlyWorker
import com.pyamsoft.pydroid.core.Enforcer
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
internal class WorkManagerButler
@Inject
internal constructor(
    private val context: Context,
) : Butler {

  @CheckResult
  private fun workManager(): WorkManager {
    Enforcer.assertOffMainThread()
    return WorkManager.getInstance(context)
  }

  @CheckResult
  private fun Order.asWork(): Class<out Worker> {
    val workClass =
        when (this) {
          is ItemOrder -> ItemWorker::class.java
          is NightlyOrder -> NightlyWorker::class.java
          else -> throw AssertionError("Invalid order to work conversion: $this")
        }

    // Basically, this is shit, but hey its Android!
    // Please make sure your orders use a class that implements a worker, thanks.
    @Suppress("UNCHECKED_CAST") return workClass as Class<out Worker>
  }

  private suspend fun queueOrder(order: Order) {
    Enforcer.assertOffMainThread()
    cancelOrder(order)

    val tag = order.tag()
    val request =
        createWorkRequest(
            work = order.asWork(),
            tag = tag,
            isPeriodic = order is PeriodicOrder,
            period = order.period(),
            inputData = order.parameters().toInputData())

    workManager().enqueue(request)
    Timber.d("Queue work [$tag]: ${request.id}")
  }

  override suspend fun placeOrder(order: Order) =
      withContext(context = Dispatchers.Default) { queueOrder(order) }

  override suspend fun cancelOrder(order: Order) =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        workManager().cancelAllWorkByTag(order.tag()).await()
      }

  override suspend fun cancel() =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        workManager().cancelAllWork().await()
      }

  companion object {
    private val PERIOD_UNIT = TimeUnit.MILLISECONDS

    private val butlerExecutor = Executor { it.run() }

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
        this.addListener(
            {
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
            },
            butlerExecutor)
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

    @CheckResult
    private fun generateConstraints(): Constraints {
      Enforcer.assertOffMainThread()
      return Constraints.Builder().setRequiresBatteryNotLow(true).build()
    }

    @JvmStatic
    @CheckResult
    private fun createWorkRequest(
        work: Class<out Worker>,
        tag: String,
        period: Long,
        isPeriodic: Boolean,
        inputData: Data
    ): WorkRequest {
      Enforcer.assertOffMainThread()

      // Need to do this or else the Kotlin compiler has a Backend error
      // If you try to assign the builder to a variable and then apply the
      // setInputData addTag setConstraints common methods.
      return if (isPeriodic) {
        PeriodicWorkRequest.Builder(work, period, PERIOD_UNIT)
            .setInputData(inputData)
            .addTag(tag)
            .setConstraints(generateConstraints())
            .build()
      } else {
        OneTimeWorkRequest.Builder(work)
            .setInitialDelay(period, PERIOD_UNIT)
            .setInputData(inputData)
            .addTag(tag)
            .setConstraints(generateConstraints())
            .build()
      }
    }
  }
}
