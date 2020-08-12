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

package com.pyamsoft.fridge.tooltip

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner

interface TipCreator<B : Any, R : Tip> {

    @CheckResult
    fun center(owner: LifecycleOwner): R

    @CheckResult
    fun center(owner: LifecycleOwner, builder: B.() -> B): R

    @CheckResult
    fun top(owner: LifecycleOwner): R

    @CheckResult
    fun top(owner: LifecycleOwner, builder: B.() -> B): R

    @CheckResult
    fun left(owner: LifecycleOwner): R

    @CheckResult
    fun left(owner: LifecycleOwner, builder: B.() -> B): R

    @CheckResult
    fun right(owner: LifecycleOwner): R

    @CheckResult
    fun right(owner: LifecycleOwner, builder: B.() -> B): R

    @CheckResult
    fun bottom(owner: LifecycleOwner): R

    @CheckResult
    fun bottom(owner: LifecycleOwner, builder: B.() -> B): R
}
