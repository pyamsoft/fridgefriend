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

import com.pyamsoft.pydroid.arch.UiRender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// TODO expose this from PYDroid
class StateUiRender<S>(private val state: S) : UiRender<S> {
    override fun render(scope: CoroutineScope, onRender: (state: S) -> Unit) {
        scope.launch(context = Dispatchers.Main) {
            onRender(state)
        }
    }

    override fun <T> distinctBy(distinctBy: (state: S) -> T): UiRender<T> {
        return StateUiRender(distinctBy(state))
    }

    override fun distinct(areEquivalent: (old: S, new: S) -> Boolean): UiRender<S> {
        return this
    }

}