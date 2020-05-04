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

package com.pyamsoft.fridge.core

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.annotation.CheckResult
import androidx.core.content.getSystemService
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import java.util.Calendar
import java.util.Date

const val PRIVACY_POLICY_URL =
    "https://pyamsoft.blogspot.com/p/fridgefriend-privacy-policy.html"
const val TERMS_CONDITIONS_URL =
    "https://pyamsoft.blogspot.com/p/fridgefriend-terms-and-conditions.html"

@CheckResult
fun currentDate(): Date {
    return today().time
}

@CheckResult
fun today(): Calendar {
    return Calendar.getInstance()
}

@CheckResult
inline fun today(func: Calendar.() -> Unit): Calendar {
    return Calendar.getInstance().apply(func)
}

@CheckResult
fun animatePopInFromBottom(view: View): ViewPropertyAnimatorCompat {
    view.translationY = animatingHeight(view.context.applicationContext)
    view.isVisible = true
    return ViewCompat.animate(view)
        .translationY(0F)
        .setDuration(700)
        .setStartDelay(300)
        .setInterpolator(interpolator)
}

private val interpolator by lazy(LazyThreadSafetyMode.NONE) { OvershootInterpolator(1.4F) }

private fun animatingHeight(context: Context): Float {
    val point = Point()
    val app = context.applicationContext
    val window = requireNotNull(app.getSystemService<WindowManager>())
    window.defaultDisplay.getSize(point)
    return point.y.toFloat()
}

fun View.applyToolbarOffset() {
    this.doOnApplyWindowInsets { v, insets, padding ->
        val toolbarTopMargin = padding.top + insets.systemWindowInsetTop
        v.context.withStyledAttributes(
            R.attr.toolbarStyle,
            intArrayOf(R.attr.actionBarSize)
        ) {
            val sizeId = getResourceId(0, 0)
            if (sizeId != 0) {
                val toolbarHeight = v.context.resources.getDimensionPixelSize(sizeId)
                v.updatePadding(top = toolbarTopMargin + toolbarHeight)
            }
        }
    }
}
