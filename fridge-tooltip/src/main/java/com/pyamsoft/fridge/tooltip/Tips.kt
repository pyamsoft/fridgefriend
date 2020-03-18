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

import android.view.MotionEvent
import android.view.View
import androidx.annotation.CheckResult
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.OnBalloonClickListener
import com.skydoves.balloon.OnBalloonOutsideTouchListener

@CheckResult
internal fun lazyBalloon(
    builder: Balloon.Builder,
    params: BalloonParameters
): Lazy<Balloon> {
    return lazy(LazyThreadSafetyMode.NONE) {
        val balloon = builder.build()

        if (params.dismissOnClick) {
            balloon.onBalloonClickListener = object : OnBalloonClickListener {
                override fun onBalloonClick(view: View) {
                    balloon.dismiss()
                }
            }
        }

        if (params.dismissOnClickOutside) {
            balloon.onBalloonOutsideTouchListener = object : OnBalloonOutsideTouchListener {
                override fun onBalloonOutsideTouch(view: View, event: MotionEvent) {
                    balloon.dismiss()
                }
            }
        }

        balloon
    }
}
