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

package com.pyamsoft.fridge.detail

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.annotation.CheckResult
import androidx.cardview.widget.CardView

// Adapted from
// https://github.com/nikhilpanju/FabFilter/blob/master/app/src/main/java/com/nikhilpanju/fabfilter/utils/CardViewAnimatorHelper.kt

// Simple value animator
@CheckResult
inline fun valueAnimator(
    forward: Boolean,
    duration: Long,
    interpolator: TimeInterpolator,
    crossinline updateListener: (progress: Float) -> Unit
): ValueAnimator {
    val from: Float
    val to: Float
    if (forward) {
        from = 0F
        to = 1F
    } else {
        from = 1F
        to = 0F
    }

    return ValueAnimator.ofFloat(from, to).apply {
        this.addUpdateListener { updateListener(it.animatedValue as Float) }
        this.duration = duration
        this.interpolator = interpolator
    }
}

class CardAnimator @JvmOverloads internal constructor(
    private val card: CardView,
    private val startWidth: Float = card.width.toFloat(),
    private val endWidth: Float = -1F,
    private val startHeight: Float = card.height.toFloat(),
    private val endHeight: Float = -1F,
    private val startX: Float = card.x,
    private val endX: Float = -1F,
    private val startY: Float = card.y,
    private val endY: Float = -1F,
    private val startElevation: Float = card.elevation,
    private val endElevation: Float = -1F,
    private val startRadius: Float = card.radius,
    private val endRadius: Float = -1F,
    private val duration: Long = 300L,
    private val interpolator: TimeInterpolator = AccelerateDecelerateInterpolator()
) {

    var progress: Float = 0F
        set(value) {
            val oldField = field
            if (oldField == value) {
                return
            }

            field = value
            if (endWidth >= 0) {
                card.layoutParams.width = getProgress(startWidth, endWidth).toInt()
            }

            if (endHeight >= 0) {
                card.layoutParams.height = getProgress(startHeight, endHeight).toInt()
            }

            if (endWidth >= 0 || endHeight >= 0) {
                card.requestLayout()
            }

            if (endX >= 0) {
                card.x = getProgress(startX, endX)
            }

            if (endY >= 0) {
                card.y = getProgress(startY, endY)
            }

            if (endRadius >= 0) {
                card.radius = getProgress(startRadius, endRadius)
            }

            if (endElevation >= 0) {
                card.cardElevation = getProgress(startElevation, endElevation)
            }
        }

    @CheckResult
    @JvmOverloads
    fun animator(
        forward: Boolean,
        listener: (progress: Float) -> Unit = DEFAULT_LISTENER
    ): ValueAnimator {
        return valueAnimator(forward, duration, interpolator) { p ->
            this.progress = p
            listener(p)
        }
    }

    @CheckResult
    private fun getProgress(start: Float, end: Float): Float {
        return start + ((end - start) * progress)
    }

    companion object {

        private val DEFAULT_LISTENER: (Float) -> Unit = {}
    }
}


