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

package com.pyamsoft.fridge.ui.pie.internal

import android.view.animation.Animation
import android.view.animation.Transformation

internal class PieChartAnimation internal constructor(
    private val chartRenderData: List<PieRenderData>,
    private val onAnimationUpdate: (List<PieRenderData>) -> Unit
) : Animation() {

    private val progressRenderData by lazy(LazyThreadSafetyMode.NONE) {
        chartRenderData.map { -90f to 0f }
    }

    override fun applyTransformation(interpolatedTime: Float, transformation: Transformation) {
        val animatedValues = chartRenderData.zip(progressRenderData) { data, values ->
            val startAngle = values.first
            val sweepAngle = values.second
            val progressSweepAngle = (data.sweepAngle - sweepAngle) * interpolatedTime
            val progressStartAngle = -90 + (data.startAngle - startAngle) * interpolatedTime
            return@zip data.copy(
                startAngle = progressStartAngle,
                sweepAngle = progressSweepAngle
            )
        }
        onAnimationUpdate(animatedValues)
    }

}

