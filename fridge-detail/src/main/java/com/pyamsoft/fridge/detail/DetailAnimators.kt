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
import androidx.annotation.CheckResult

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


