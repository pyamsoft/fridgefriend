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

package com.pyamsoft.fridge.tooltip

import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange

interface TooltipBuilder {

    @CheckResult
    fun dismissOnClick(): TooltipBuilder

    @CheckResult
    fun dismissOnClickOutside(): TooltipBuilder

    @CheckResult
    fun setText(text: String): TooltipBuilder

    @CheckResult
    fun setIcon(icon: Drawable): TooltipBuilder

    @CheckResult
    fun setIconRes(@DrawableRes icon: Int): TooltipBuilder

    @CheckResult
    fun setBackground(@ColorInt color: Int): TooltipBuilder

    @CheckResult
    fun setBackgroundRes(@ColorRes color: Int): TooltipBuilder

    @CheckResult
    fun setTextColor(@ColorInt color: Int): TooltipBuilder

    @CheckResult
    fun setTextColorRes(@ColorRes color: Int): TooltipBuilder

    @CheckResult
    fun setArrowPosition(@FloatRange(from = 0.0, to = 1.0) value: Float): TooltipBuilder

    @CheckResult
    fun setTextSize(value: Float): TooltipBuilder

    @CheckResult
    fun setAnimation(animation: Tip.Animation?): TooltipBuilder
}
