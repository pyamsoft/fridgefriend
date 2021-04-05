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

package com.pyamsoft.fridge.ui

import android.content.Context
import android.graphics.Point
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible

private val overshootInterpolator by lazy(LazyThreadSafetyMode.NONE) { OvershootInterpolator(1.4F) }

@CheckResult
private fun animatingHeight(activityContext: Context): Int {
    val windowManager = requireNotNull(activityContext.getSystemService<WindowManager>())
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        windowManager.currentWindowMetrics.bounds.bottom
    } else {
        val point = Point()

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getSize(point)

        point.y
    }
}

@CheckResult
@JvmOverloads
fun animatePopInFromBottom(view: View, delay: Long = 300L, overshoot: Boolean = true): ViewPropertyAnimatorCompat {
    view.translationY = animatingHeight(view.context).toFloat()
    view.scaleX = 0.4F
    view.isVisible = true
    return ViewCompat.animate(view).apply {
        translationY(0F)
        scaleX(1.0F)
        duration = 700
        startDelay = delay
        if (overshoot) {
            interpolator = overshootInterpolator
        }
    }
}
