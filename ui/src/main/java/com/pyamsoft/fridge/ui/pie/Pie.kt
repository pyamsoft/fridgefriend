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

package com.pyamsoft.fridge.ui.pie

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.CheckResult
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.pyamsoft.fridge.ui.pie.internal.PieChartAnimation
import com.pyamsoft.fridge.ui.pie.internal.PiePreview
import com.pyamsoft.fridge.ui.pie.internal.PieRenderData
import com.pyamsoft.fridge.ui.pie.internal.toPx
import timber.log.Timber

/**
 * Pie Chart
 *
 * Modified from https://github.com/creati8e/Finances
 *
 * Original copyright Sergey Chuprin (Apache-v2)
 */
class Pie @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var renderData = listOf<PieRenderData>()
    private var progressRenderData = listOf<PieRenderData>()

    private val chartArea by lazy(LazyThreadSafetyMode.NONE) { RectF() }
    private val partSize by lazy(LazyThreadSafetyMode.NONE) { context.toPx(PORTION_SIZE_DP) }

    private val dataPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = partSize
        }
    }

    init {
        if (isInEditMode) {
            renderData = buildRenderData(PiePreview.PREVIEW_DATA)
            redraw(animate = false)
        }
    }

    /**
     * Set the pie data with a vararg list of Parts
     *
     * Triggers re-render
     */
    fun setData(vararg data: PieData.Part) {
        setData(data.toList())
    }

    /**
     * Set the pie data with a list of Parts
     *
     * Triggers re-render
     */
    fun setData(data: List<PieData.Part>) {
        renderData = buildRenderData(data)
        redraw(animate = true)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)

        val minDimension = minOf(measuredWidth, measuredHeight)

        val offset = partSize / 2
        val minOffset = minDimension - offset
        chartArea.set(
            offset + paddingStart,
            offset + paddingTop,
            minOffset - paddingEnd,
            minOffset - paddingEnd
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw pie itself.
        renderData.forEachIndexed { i, data ->
            dataPaint.color = data.color.color
            val renderPoint = progressRenderData[i]
            canvas.drawArc(
                chartArea,
                renderPoint.startAngle,
                renderPoint.sweepAngle,
                false,
                dataPaint
            )
        }
    }

    private fun redraw(animate: Boolean) {
        if (animate) {
            val animation = PieChartAnimation(renderData) { values ->
                progressRenderData = values
                requestLayout()
            }

            animation.run {
                duration = ANIMATION_DURATION
                interpolator = ANIMATION_INTERPOLATOR
                startAnimation(this)
            }
        } else {
            progressRenderData = renderData
            invalidate()
        }
    }

    private fun buildRenderData(parts: List<PieData.Part>): List<PieRenderData> {
        val maxValue = getMaxValue(parts)
        // If only single portion is displayed, remove the gap.
        if (parts.size == 1) {
            val part = parts.first()
            return listOf(
                PieRenderData(
                    startAngle = -90f,
                    color = part.color,
                    sweepAngle = (part.data.value * 360 / maxValue).toDisplayValue()
                )
            )
        }

        return mutableListOf<PieRenderData>().apply {
            parts.sortedByDescending { it.data.value }.forEach { part ->
                val startAngle = if (this.isEmpty()) -90f else {
                    val last = this.last()
                    last.startAngle + last.sweepAngle + DISTANCE_BETWEEN_PARTS
                }

                // Ensure that displayed portions is not very small.
                val sweepAngle = (part.data.value * 360 / maxValue - DISTANCE_BETWEEN_PARTS)

                this.add(
                    PieRenderData(
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.toDisplayValue(),
                        color = part.color
                    )
                )
            }
        }
    }

    @CheckResult
    private fun Float.toDisplayValue(): Float {
        return this.coerceAtLeast(2F)
    }

    companion object {

        private const val PORTION_SIZE_DP = 15
        private const val ANIMATION_DURATION = 1000L
        private const val DISTANCE_BETWEEN_PARTS = 0F

        private val ANIMATION_INTERPOLATOR by lazy(LazyThreadSafetyMode.NONE) {
            FastOutSlowInInterpolator()
        }

        @JvmStatic
        @CheckResult
        private fun getMaxValue(parts: List<PieData.Part>): Float {
            return parts.fold(0F, { acc, v -> acc + v.data.value })
        }
    }
}
