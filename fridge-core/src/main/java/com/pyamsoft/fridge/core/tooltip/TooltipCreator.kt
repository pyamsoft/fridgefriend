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

interface TooltipCreator {

    @CheckResult
    fun center(): Tooltip

    @CheckResult
    fun center(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun top(): Tooltip

    @CheckResult
    fun top(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun left(): Tooltip

    @CheckResult
    fun left(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun right(): Tooltip

    @CheckResult
    fun right(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip

    @CheckResult
    fun bottom(): Tooltip

    @CheckResult
    fun bottom(builder: TooltipBuilder.() -> TooltipBuilder): Tooltip
}
