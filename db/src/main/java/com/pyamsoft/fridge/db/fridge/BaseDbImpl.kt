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

package com.pyamsoft.fridge.db.fridge

import com.pyamsoft.fridge.db.BaseRealtime
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal abstract class BaseDbImpl<T : Any, ChangeEvent : Any> protected constructor() :
    BaseRealtime<ChangeEvent> {

    private val bus = EventBus.create<ChangeEvent>(emitOnlyWhenActive = true)

    protected suspend fun onEvent(onEvent: suspend (event: ChangeEvent) -> Unit) =
        withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext bus.onEvent(onEvent)
        }

    final override suspend fun publish(event: ChangeEvent) = withContext(context = Dispatchers.IO) {
        Enforcer.assertOffMainThread()
        invalidate()
        bus.send(event)
    }
}
