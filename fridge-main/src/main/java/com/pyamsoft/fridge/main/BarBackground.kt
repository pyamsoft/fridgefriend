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

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.drawable.Drawable
import androidx.annotation.CheckResult
import androidx.annotation.ColorInt

internal abstract class BarBackground protected constructor(
    @ColorInt private val barColor: Int
) : Drawable() {

    private val paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val hole by lazy(LazyThreadSafetyMode.NONE) { holePunch() }

    final override fun draw(canvas: Canvas) {
        canvas.drawColor(barColor)
        hole?.also { h ->
            canvas.drawCircle(h.location, 0F, h.radius, paint)
        }
    }

    final override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    final override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    final override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @CheckResult
    protected abstract fun holePunch(): HolePunch?

    protected data class HolePunch internal constructor(
        val location: Float,
        val radius: Float
    )
}
