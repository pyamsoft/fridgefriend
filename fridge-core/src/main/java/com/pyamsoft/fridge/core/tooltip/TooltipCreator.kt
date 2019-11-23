/*
 * Copyright 2019 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.core.tooltip

import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner

interface TooltipCreator {

    @CheckResult
    fun center(owner: LifecycleOwner): Tooltip

    @CheckResult
    fun center(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun top(owner: LifecycleOwner): Tooltip

    @CheckResult
    fun top(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun left(owner: LifecycleOwner): Tooltip

    @CheckResult
    fun left(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun right(owner: LifecycleOwner): Tooltip

    @CheckResult
    fun right(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun bottom(owner: LifecycleOwner): Tooltip

    @CheckResult
    fun bottom(owner: LifecycleOwner, builder: TooltipBuilder.() -> TooltipBuilder): Tooltip
}
