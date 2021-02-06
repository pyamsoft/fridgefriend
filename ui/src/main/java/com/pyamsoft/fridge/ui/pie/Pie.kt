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
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.CheckResult
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.pyamsoft.fridge.ui.pie.internal.PieChartAnimation
import com.pyamsoft.fridge.ui.pie.internal.PiePreview
import com.pyamsoft.fridge.ui.pie.internal.PieRenderData
import com.pyamsoft.fridge.ui.pie.internal.getAttributeColor
import com.pyamsoft.fridge.ui.pie.internal.toPx

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

    private var title = ""
    private var description = ""

    private var renderData = listOf<PieRenderData>()
    private var progressRenderData = listOf<PieRenderData>()

    private val chartArea by lazy(LazyThreadSafetyMode.NONE) { RectF() }
    private val partSize by lazy(LazyThreadSafetyMode.NONE) { context.toPx(PORTION_SIZE_DP) }

    // Optimization for text paint instead of fetching this twice
    private val textPaintColor by lazy(LazyThreadSafetyMode.NONE) {
        if (isInEditMode) Color.BLACK else {
            context.getAttributeColor(android.R.attr.textColorPrimary)
        }
    }

    private val dataPaint by lazy(LazyThreadSafetyMode.NONE) {
        Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = partSize
        }
    }

    private val titlePaint by lazy(LazyThreadSafetyMode.NONE) {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = context.toPx(PRIMARY_TEXT_SIZE_SP)
            color = textPaintColor
        }
    }

    private val descriptionPaint by lazy(LazyThreadSafetyMode.NONE) {
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = context.toPx(SECONDARY_TEXT_SIZE_SP)
            color = textPaintColor
        }
    }

    init {
        if (isInEditMode) {
            initializeWith(
                PiePreview.PREVIEW_DATA,
                title = "Example Pie",
                description = "Look at my pie!"
            )
        }
    }

    /**
     * Initialize the chart with pre-prepared data.
     *
     * Initialization will not animate.
     */
    fun initializeWith(
        values: List<PieData.Data>,
        colors: List<PieData.Color>,
        title: String,
        description: String,
    ) = initializeWith(zipToParts(values, colors), title, description)

    /**
     * Initialize the chart with pre-prepared data.
     *
     * Initialization will not animate.
     */
    fun initializeWith(
        data: List<PieData.Part>,
        title: String,
        description: String,
    ) {
        this.title = title
        this.description = description
        renderData = buildRenderData(data)
        redraw(animate = false)
    }

    /**
     * Set the pie data with a list of values and colors
     *
     * Lists must be the same length
     *
     * Triggers re-render
     */
    fun setData(
        values: List<PieData.Data>,
        colors: List<PieData.Color>
    ) = setData(zipToParts(values, colors))

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

    /**
     * Set the pie title
     *
     * Triggers re-render
     */
    fun setTitle(title: String) {
        this.title = title
        redraw(animate = true)
    }

    /**
     * Set the pie description
     *
     * Triggers re-render
     */
    fun setDescription(description: String) {
        this.description = description
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

        // Draw title
        canvas.drawText(
            title,
            getTextCenterX(title, titlePaint),
            getCenterY(),
            titlePaint
        )

        // Draw description
        canvas.drawText(
            description,
            getTextCenterX(description, descriptionPaint),
            getCenterY() + titlePaint.textSize,
            descriptionPaint
        )

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

    @CheckResult
    private fun getCenterY(): Float {
        return chartArea.centerY()
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
    private fun getTextCenterX(text: String, paint: Paint): Float {
        val textWidth = paint.measureText(text)
        return (measuredWidth + partSize - textWidth) / 2F - paint.textSize / 2F
    }

    @CheckResult
    private fun Float.toDisplayValue(): Float {
        return this.coerceAtLeast(2F)
    }

    companion object {

        private const val PRIMARY_TEXT_SIZE_SP = 20F
        private const val SECONDARY_TEXT_SIZE_SP = 14F

        private const val PORTION_SIZE_DP = 15
        private const val ANIMATION_DURATION = 1000L
        private const val DISTANCE_BETWEEN_PARTS = 4F

        private val ANIMATION_INTERPOLATOR by lazy(LazyThreadSafetyMode.NONE) {
            FastOutSlowInInterpolator()
        }

        @JvmStatic
        @CheckResult
        private fun getMaxValue(parts: List<PieData.Part>): Float {
            return parts.fold(0F, { acc, v -> acc + v.data.value })
        }

        @JvmStatic
        @CheckResult
        private fun zipToParts(
            values: List<PieData.Data>,
            colors: List<PieData.Color>
        ): List<PieData.Part> {
            return values.zip(colors) { value, color -> PieData.Part(value, color) }
        }
    }
}
