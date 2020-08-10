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

package com.pyamsoft.fridge.butler.injector

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.params.BaseParameters
import com.pyamsoft.fridge.butler.runner.WorkResult
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

abstract class BaseInjector<P : BaseParameters> protected constructor(context: Context) {

    private var applicationContext: Context? = context.applicationContext

    @CheckResult
    suspend fun run(
        id: UUID,
        tags: Set<String>,
        params: P
    ): WorkResult {
        val result = withContext(context = Dispatchers.Default) {
            onRun(requireNotNull(applicationContext), id, tags, params)
        }
        applicationContext = null
        return result
    }

    @CheckResult
    protected abstract suspend fun onRun(
        context: Context,
        id: UUID,
        tags: Set<String>,
        params: P
    ): WorkResult
}
