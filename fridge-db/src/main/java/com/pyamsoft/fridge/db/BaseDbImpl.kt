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

package com.pyamsoft.fridge.db

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal abstract class BaseDbImpl<T : Any, ChangeEvent : Any> protected constructor(
    private val enforcer: Enforcer,
    private val cache: Cached1<Sequence<T>, Boolean>
) {

    private val bus = EventBus.create<ChangeEvent>()

    protected suspend fun onEvent(onEvent: suspend (event: ChangeEvent) -> Unit) {
        return bus.onEvent(onEvent)
    }

    protected suspend fun publishRealtime(event: ChangeEvent) {
        enforcer.assertNotOnMainThread()
        invalidate()
        publish(event)
    }

    fun invalidate() {
        cache.clear()
    }

    suspend fun publish(event: ChangeEvent) =
        withContext(context = Dispatchers.IO) {
            enforcer.assertNotOnMainThread()
            bus.send(event)
        }
}
