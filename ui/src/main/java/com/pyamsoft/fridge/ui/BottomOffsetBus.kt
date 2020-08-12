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

package com.pyamsoft.fridge.ui

import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
internal class BottomOffsetBus @Inject internal constructor() : EventBus<BottomOffset> {

    private val bus = EventBus.create<BottomOffset>()
    private val mutex = Mutex()
    private var mostRecentEvent: BottomOffset? = null

    override suspend fun onEvent(emitter: suspend (event: BottomOffset) -> Unit) {
        Enforcer.assertOffMainThread()

        mutex.withLock {
            mostRecentEvent?.also { emitter(it) }
        }
        return bus.onEvent(emitter)
    }

    override suspend fun send(event: BottomOffset) {
        Enforcer.assertOffMainThread()

        mutex.withLock { mostRecentEvent = event }
        bus.send(event)
    }
}
