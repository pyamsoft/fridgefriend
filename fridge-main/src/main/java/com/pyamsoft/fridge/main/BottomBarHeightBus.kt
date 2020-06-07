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

package com.pyamsoft.fridge.main

import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class BottomBarHeightBus @Inject internal constructor() : EventBus<BottomBarHeight> {

    private val bus = EventBus.create<BottomBarHeight>()
    private val mutex = Mutex()
    private var mostRecentEvent: BottomBarHeight? = null

    override suspend fun onEvent(emitter: suspend (event: BottomBarHeight) -> Unit) {
        mutex.withLock {
            mostRecentEvent?.also { emitter(it) }
        }
        return bus.onEvent(emitter)
    }

    override fun publish(event: BottomBarHeight) {
        throw NotImplementedError("Do not use this, use send()")
    }

    override suspend fun send(event: BottomBarHeight) {
        mutex.withLock { mostRecentEvent = event }
        bus.send(event)
    }
}
