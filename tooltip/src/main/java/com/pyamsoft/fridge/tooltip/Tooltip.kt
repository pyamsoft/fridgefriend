/*
 * Copyright 2021 Peter Kenji Yamanaka
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

import android.graphics.drawable.Drawable
import android.view.View
import androidx.annotation.CheckResult
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

interface Tooltip : Hideable {

    fun show(anchor: View)

    fun show(anchor: View, xOff: Int, yOff: Int)

    enum class Direction {
        CENTER,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT
    }

    interface Builder {

        @CheckResult
        fun setText(text: String): Builder

        @CheckResult
        fun setText(@StringRes text: Int): Builder

        @CheckResult
        fun setIcon(icon: Drawable): Builder

        @CheckResult
        fun setIconRes(@DrawableRes icon: Int): Builder
    }
}
